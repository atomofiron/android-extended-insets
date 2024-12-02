/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi


const val INVALID_INSETS_LISTENER_KEY = 0

class InsetsProviderImpl(
    private var dropNative: Boolean,
) : InsetsProvider, InsetsListener, ViewDelegate, View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

    private var source = ExtendedWindowInsets.Empty
        set(value) {
            logd { "$nameWithId new received? ${field != value}" }
            if (field != value) {
                field = value
                updateCurrent(value)
            }
        }
    private var modified = ExtendedWindowInsets.Empty
    override var current = ExtendedWindowInsets.Empty
        private set(value) {
            isActual = true
            logd { "$nameWithId new current? ${field != value}" }
            if (field != value) {
                val prev = field
                field = value
                notifyListeners(prev, value)
            }
        }
    override var view: View? = null
        private set
    override var nameWithId: String = this::class.simpleName.toString()
        private set

    private val listeners = hashMapOf<Int, InsetsListener>()
    private val sources = hashMapOf<Int, InsetsSource>()
    private var insetsModifier: InsetsModifierCallback? = null
    private var parentProvider: InsetsProvider? = null
    private val typesProvider = ::collectTypes
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()
    // prevent extra notifications of listeners
    private val isInLayout get() = view?.isInLayout ?: false
    private var isNotifying = false
    private var isRequested = false
    // some cached modifiers were changed, insets rebuilding required
    private var isActual = true

    constructor() : this(dropNative = false)

    constructor(context: Context, attrs: AttributeSet?) : this(context.dropNativeInsets(attrs))

    override fun View.onInit() {
        view = this
        nameWithId = nameWithId()
        addOnAttachStateChangeListener(this@InsetsProviderImpl)
        addOnLayoutChangeListener(this@InsetsProviderImpl)
        if (isAttachedToWindow) onViewAttachedToWindow(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        parentProvider = view.parent.findInsetsProvider()
        logd { "$nameWithId was attached, parent provider? ${parentProvider != null}" }
        parentProvider?.addInsetsListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "$nameWithId was detached, parent provider? ${parentProvider != null}" }
        parentProvider?.removeInsetsListener(this)
        parentProvider = null
    }

    override fun onLayoutChange(view: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int) {
        if (isRequested) {
            logd { "$nameWithId notify listeners after layout changed" }
            updateCurrent(source)
        }
    }

    override fun setInsetsModifier(modifier: InsetsModifierCallback) {
        logd { "$nameWithId set modifier" }
        insetsModifier = modifier
        updateCurrent(source)
    }

    override fun addInsetsListener(listener: InsetsListener): Int {
        if (listener === view || listener === this) {
            throw IllegalArgumentException()
        }
        listeners.entries.forEach { entry ->
            if (entry.value === listener) {
                logd { "$nameWithId listener already added" }
                return entry.key
            } else if (listener.checkTheSameView(entry.value)) {
                MultipleViewInsetsDelegate("The target view ${listener.view()?.nameWithId()}").printStackTrace()
                return INVALID_INSETS_LISTENER_KEY
            }
        }
        logd { "$nameWithId add listener -> ${listeners.size.inc()}" }
        val key = nextKey++
        listeners[key] = listener
        listener.onApplyWindowInsets(current)
        return key
    }

    override fun removeInsetsListener(listener: InsetsListener) {
        val entry = listeners.entries.find { it.value === listener }
        entry ?: return logd { "$nameWithId listener not found" }
        removeInsetsListener(entry.key)
    }

    override fun removeInsetsListener(key: Int) {
        val removed = listeners.remove(key) != null
        logd { "$nameWithId remove listener? $removed -> ${listeners.size}" }
    }

    // the one of the two entry points for window insets
    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (parentProvider == null) {
            logd { "$nameWithId native insets were received" }
            source = ExtendedWindowInsets(windowInsets, view)
        }
        return if (dropNative) WindowInsets.CONSUMED else windowInsets
    }

    override fun requestInsets() = when {
        isInLayout -> isRequested = true.also { logd { "$nameWithId insets were requested during the layout" } }
        isNotifying -> isRequested = true.also { logd { "$nameWithId insets were requested during the notification of listeners" } }
        else -> updateCurrent(source)
    }

    override fun submitInsets(key: Int, source: InsetsSource) {
        val were = sources[key]
        if (were == source) {
            return logd { "$nameWithId insets were not changed: $source" }
        }
        sources[key] = source
        logd { "$nameWithId insets were updated from $were -> $source" }
        when {
            isNotifying || isRequested -> isActual = false
            else -> updateWithSources()
        }
    }

    override fun revokeInsetsFrom(key: Int) {
        val source = sources.remove(key)
        source ?: return
        when {
            source.isEmpty() -> Unit
            isNotifying || isRequested -> isActual = false
            else -> updateWithSources()
        }
    }

    override fun dropNativeInsets(drop: Boolean) {
        logd { "$nameWithId native insets discarding has been ${if (drop) "enabled" else "disabled"}" }
        dropNative = drop
    }

    override fun collectTypes(): TypeSet {
        var types = TypeSet.Empty
        for (listener in listeners.values) {
            types += when {
                listener.types.isNotEmpty() -> listener.types
                listener is InsetsProvider -> listener.collectTypes()
                else -> continue
            }
        }
        return types
    }

    // the one of the two entry points for window insets
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        source = windowInsets
    }

    private fun updateCurrent(source: ExtendedWindowInsets) {
        logd { "$nameWithId update current, with modifier? ${insetsModifier != null}" }
        isRequested = false
        modified = insetsModifier?.modify(typesProvider, source) ?: source
        updateWithSources()
    }

    private fun updateWithSources() {
        logd { "$nameWithId update current with ${sources.size} sources" }
        var builder: ExtendedBuilder? = null
        for (entry in sources) {
            if (entry.value.isNotEmpty()) {
                builder = (builder ?: modified.builder()).applySources(entry.value)
            }
        }
        current = builder?.build() ?: modified
    }

    private fun notifyListeners(prev: ExtendedWindowInsets, new: ExtendedWindowInsets) {
        logd { "$nameWithId notify ${listeners.size} listeners" }
        isNotifying = true
        val listeners = listeners.values.toTypedArray()
        val currentViewDelegateIndex = listeners.indexOfFirst { it.view() === view }
        listeners.getOrNull(currentViewDelegateIndex)?.onApplyWindowInsets(source)
        listeners.forEachIndexed { index, it ->
            when {
                index == currentViewDelegateIndex -> Unit
                it.types.isEmpty() -> it.onApplyWindowInsets(new)
                it.types.any { prev[it] != new[it] } -> it.onApplyWindowInsets(new)
            }
        }
        if (isRequested) {
            logd { "$nameWithId restart full insets update" }
            updateCurrent(source)
        } else if (isActual) {
            logd { "$nameWithId restart insets update from cache" }
            updateWithSources()
        }
        isNotifying = false
    }
}

private fun Any?.view(): View? = (this as? ViewDelegate)?.view

private fun ExtendedBuilder.applySources(sources: InsetsSource): ExtendedBuilder {
    val debugData = (sources as? InsetsSource)?.debugData
    for (source in sources) {
        logd { "$debugData source: $source" }
        max(source.types, source.insets)
    }
    return this
}

private fun InsetsListener.checkTheSameView(other: InsetsListener) = when {
    this !is ViewInsetsDelegateImpl -> false
    other !is ViewInsetsDelegateImpl -> false
    else -> other.view === view
}

private fun Context.dropNativeInsets(attrs: AttributeSet?): Boolean {
    val array = obtainStyledAttributes(attrs, intArrayOf(R.attr.dropNativeInsets))
    return array.getBoolean(0, false).also { array.recycle() }
}
