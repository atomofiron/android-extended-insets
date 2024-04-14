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

class InsetsProviderImpl private constructor(
    private var dropNative: Boolean,
) : InsetsProvider, InsetsListener, View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

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

    private val listeners = hashMapOf<Int, InsetsListener>()
    private val sources = hashMapOf<Int, InsetsSource>()
    private var insetsModifier: InsetsModifierCallback? = null
    private var parentProvider: InsetsProvider? = null
    private var thisView: View? = null
    private var nameWithId: String? = null
    private val typesProvider = ::collectTypes
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()
    // prevent extra notifications of listeners
    private val isInLayout get() = thisView?.isInLayout ?: false
    private var isNotifying = false
    private var isRequested = false
    // some cached modifiers were changed, insets rebuilding required
    private var isActual = true

    constructor() : this(dropNative = false)

    constructor(context: Context, attrs: AttributeSet?) : this(context.dropNativeInsets(attrs))

    override fun View.onInit() {
        thisView = this
        nameWithId = nameWithId()
        addOnAttachStateChangeListener(this@InsetsProviderImpl)
        addOnLayoutChangeListener(this@InsetsProviderImpl)
        if (isAttachedToWindow) onViewAttachedToWindow(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        parentProvider = view.parent.findInsetsProvider()
        this.logd { "$nameWithId onAttach parent provider? ${parentProvider != null}" }
        parentProvider?.addInsetsListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "$nameWithId onDetach parent provider? ${parentProvider != null}" }
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
        if (listener === thisView || listener === this) {
            throw IllegalArgumentException()
        }
        listeners.entries.forEach { entry ->
            if (entry.value === listener) {
                logd { "$nameWithId listener already added" }
                return entry.key
            } else {
                listener.checkTheSameView(entry.value)
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
        val source = sources.remove(key)
        if (!source.isNullOrEmpty() && source.any { it.insets.isNotEmpty() }) {
            updateWithSources()
        }
    }

    // the one of the two entry points for window insets
    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (parentProvider == null) {
            logd { "$nameWithId native insets were received" }
            source = ExtendedWindowInsets(windowInsets, thisView)
        }
        return if (dropNative) WindowInsets.CONSUMED else windowInsets
    }

    override fun requestInsets() = when {
        isInLayout -> isRequested = true.also { logd { "$nameWithId insets were requested during the layout" } }
        isNotifying -> isRequested = true.also { logd { "$nameWithId insets were requested during the notification of listeners" } }
        else -> updateCurrent(source)
    }

    override fun publishInsetsFrom(callback: InsetsSourceCallback) {
        val entry = listeners.entries.find { it.value === callback }
            ?: return logd { "InsetsDependencyCallback was not found!" }
        publishInsets(entry.key, callback)
    }

    override fun publishInsetsFrom(view: View) {
        val entry = listeners.entries.find { it.value is InsetsSourceCallback && it.value.view() === view }
            ?: return logd { "InsetsDependencyCallback with ${view.nameWithId()} was not found!" }
        publishInsets(entry.key, entry.value as InsetsSourceCallback)
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
        var builder: ExtendedBuilder? = null
        for (entry in sources) {
            if (entry.value.isNotEmpty()) {
                builder = (builder ?: modified.builder()).applySources(listeners[entry.key], entry.value)
            }
        }
        current = builder?.build() ?: modified
    }

    private fun publishInsets(key: Int, callback: InsetsSourceCallback) {
        when (val new = callback.getSource(modified)) {
            sources[key] -> return
            null -> sources.remove(key)
            else -> sources[key] = new
        }
        when {
            isNotifying || isRequested -> isActual = false
            else -> updateWithSources()
        }
    }

    private fun notifyListeners(prev: ExtendedWindowInsets, new: ExtendedWindowInsets) {
        isNotifying = true
        val listeners = listeners.values.toTypedArray()
        val currentViewDelegateIndex = listeners.indexOfFirst { it.view() === thisView }
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

private fun Any?.view(): View? = (this as? ViewInsetsDelegateImpl)?.view

private fun ExtendedBuilder.applySources(listener: InsetsListener?, sources: InsetsSource): ExtendedBuilder {
    for (source in sources) {
        logd { "${listener.view()?.nameWithId() ?: listener?.simpleName} source: $source" }
        max(source.types, source.insets)
    }
    return this
}

private fun InsetsListener.checkTheSameView(other: InsetsListener) = when {
    this !is ViewInsetsDelegateImpl -> Unit
    other !is ViewInsetsDelegateImpl -> Unit
    other.view !== view -> Unit
    else -> throw MultipleViewInsetsDelegate("The target view ${view.nameWithId()}")
}

private fun Context.dropNativeInsets(attrs: AttributeSet?): Boolean {
    val array = obtainStyledAttributes(attrs, intArrayOf(R.attr.dropNativeInsets))
    return array.getBoolean(0, false).also { array.recycle() }
}
