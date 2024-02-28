/*
 * Copyright 2024 Yaroslav Nesterov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.atomofiron.insets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import lib.atomofiron.insets.ExtendedWindowInsets.Companion.Builder
import androidx.core.view.WindowInsetsCompat.Type as CompatType
import lib.atomofiron.insets.ExtendedWindowInsets.Type


const val INVALID_INSETS_LISTENER_KEY = 0

class InsetsProviderImpl private constructor(
    private var dropNative: Boolean,
) : InsetsProvider, InsetsListener, InsetsDependencyCallback, View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

    private var transformed = ExtendedWindowInsets.EMPTY
    private var source = ExtendedWindowInsets.EMPTY
        set(value) {
            logd { "$nameWithId new received? ${field != value}" }
            if (field != value) {
                field = value
                updateCurrent(value)
            }
        }
    override var current = ExtendedWindowInsets.EMPTY
        private set(value) {
            logd { "$nameWithId new current? ${field != value}" }
            if (field != value) {
                field = value
                notifyListeners(value)
            }
        }
    private var globalModifiers: InsetsModifier = InsetsModifier

    private val listeners = hashMapOf<Int, InsetsListener>()
    private var insetsModifier: InsetsModifierCallback? = null
    private var parentProvider: InsetsProvider? = null
    private var thisView: View? = null
    private var nameWithId: String? = null
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()
    // prevent extra notifications of listeners
    private val isInLayout get() = thisView?.isInLayout ?: false
    private var isNotifying = false
    private var isRequested = false

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
            isRequested = false
            updateCurrent(source)
        }
    }

    override fun setInsetsModifier(modifier: InsetsModifierCallback) {
        logd { "$nameWithId set modifier" }
        insetsModifier = modifier
        updateCurrent(source)
    }

    override fun getModifier(windowInsets: ExtendedWindowInsets): InsetsModifier? {
        var modifiers = globalModifiers
        for (listener in listeners.values) {
            if (listener is InsetsProviderImpl) {
                modifiers += listener.getModifier(windowInsets)
            }
        }
        return modifiers.takeIf { !it.isEmpty() }
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
    }

    // the one of the two entry points for window insets
    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (parentProvider == null) {
            logd { "$nameWithId native insets were accepted" }
            val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, thisView)
            val barsWithCutout = windowInsetsCompat.getInsets(CompatType.systemBars() or CompatType.displayCutout())
            source = Builder(windowInsetsCompat)
                .set(Type.general, barsWithCutout)
                .build()
        }
        return if (dropNative) WindowInsets.CONSUMED else windowInsets
    }

    override fun requestInsets() = when {
        isInLayout -> isRequested = true.also { logd { "$nameWithId insets were requested during the layout" } }
        isNotifying -> isRequested = true.also { logd { "$nameWithId insets were requested during the notification of listeners" } }
        else -> updateCurrent(source)
    }

    override fun dropNativeInsets(drop: Boolean) {
        logd { "$nameWithId native insets discarding has been ${if (drop) "enabled" else "disabled"}" }
        dropNative = drop
    }

    // the one of the two entry points for window insets
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        source = windowInsets
    }

    private fun updateCurrent(source: ExtendedWindowInsets) {
        logd { "$nameWithId update current, with modifier? ${insetsModifier != null}" }
        isNotifying = true
        current = insetsModifier
            ?.transform(listeners.isNotEmpty(), source)
            .let {
                transformed = it ?: source
                iterateCallbacks(transformed)
            }
        isNotifying = false
    }

    private fun iterateCallbacks(windowInsets: ExtendedWindowInsets): ExtendedWindowInsets {
        val callbacks = listeners.values.mapNotNull { it as? InsetsDependencyCallback }
        var builder: ExtendedBuilder? = null
        globalModifiers = InsetsModifier
        for (callback in callbacks) {
            callback.getModifier(windowInsets)
                ?.takeIf { it.isNotEmpty() }
                ?.let { modifier ->
                    builder = (builder ?: windowInsets.builder()).applyReversed(callback, modifier)
                    when {
                        parentProvider == null -> Unit
                        !modifier.global -> Unit
                        callback is InsetsProviderImpl -> Unit
                        else -> globalModifiers += modifier
                    }
                }
        }
        return builder?.build() ?: windowInsets
    }

    private fun notifyListeners(windowInsets: ExtendedWindowInsets) {
        isNotifying = true
        val listeners = listeners.values.toTypedArray()
        val currentViewDelegateIndex = listeners.indexOfFirst { it.view() === thisView }
        listeners.getOrNull(currentViewDelegateIndex)?.onApplyWindowInsets(transformed)
        listeners.forEachIndexed { index, it ->
            if (index != currentViewDelegateIndex) it.onApplyWindowInsets(windowInsets)
        }
        if (isRequested) {
            logd { "$nameWithId notify listeners after the notification of listeners" }
            isRequested = false
            updateCurrent(source)
        }
        isNotifying = false
    }
}

private fun Any?.view(): View? = (this as? ViewInsetsDelegateImpl)?.view

private fun ExtendedBuilder.applyReversed(callback: InsetsDependencyCallback, modifier: InsetsModifier): ExtendedBuilder {
    modifier.next?.let { applyReversed(callback, it) }
    logd { "${callback.view()?.nameWithId() ?: callback.simpleName} modifications: $modifier" }
    when (modifier.action) {
        ModifierAction.Max -> max(modifier.types, modifier.insets)
        ModifierAction.Set -> set(modifier.types, modifier.insets)
        ModifierAction.Add -> add(modifier.types, modifier.insets)
        ModifierAction.Consume -> consume(modifier.types, modifier.insets)
        ModifierAction.None -> Unit // unreachable
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
