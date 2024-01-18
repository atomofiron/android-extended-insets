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

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat


const val INVALID_INSETS_LISTENER_KEY = 0

private data class SrcState(
    val windowInsets: ExtendedWindowInsets = ExtendedWindowInsets.CONSUMED,
    val hasModifier: Boolean = false,
    val hasListeners: Boolean = false,
    val dependencies: Int = 0,
)

internal class InsetsProviderImpl : InsetsProvider, View.OnAttachStateChangeListener {

    private var srcState = SrcState()
        set(value) {
            logd { "${thisView?.nameWithId()} new state? ${field != value}" }
            if (value.hasModifier && value.dependencies > 0 || field != value) {
                field = value
                updateCurrent(value)
            }
        }
    override var current = ExtendedWindowInsets.CONSUMED
        private set(value) {
            logd { "${thisView?.nameWithId()} new current? ${field != value}" }
            if (field != value) {
                field = value
                notifyListeners(value)
            }
        }
    private val listeners = hashMapOf<Int, InsetsListener>()
    private var insetsModifier: InsetsModifier? = null
    private var provider: InsetsProvider? = null
    private var thisView: View? = null
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()

    override fun View.onInit() {
        thisView = this
        addOnAttachStateChangeListener(this@InsetsProviderImpl)
        if (isAttachedToWindow) onViewAttachedToWindow(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        provider = view.parent.findInsetsProvider()
        this.logd { "${view.nameWithId()} onAttach parent provider? ${provider != null}" }
        provider?.addInsetsListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "${view.nameWithId()} onDetach parent provider? ${provider != null}" }
        provider?.removeInsetsListener(this)
        provider = null
    }

    override fun setInsetsModifier(modifier: InsetsModifier) {
        logd { "${thisView?.nameWithId()} set modifier" }
        insetsModifier = modifier
        srcState = srcState.copy(hasModifier = true)
    }

    override fun addInsetsListener(listener: InsetsListener): Int {
        if (listener === thisView || listener === this) {
            throw IllegalArgumentException()
        }
        listeners.entries.forEach { entry ->
            if (entry.value === listener) {
                logd { "${thisView?.nameWithId()} listener already added" }
                return entry.key
            } else {
                listener.checkTheSameView(entry.value)
            }
        }
        logd { "${thisView?.nameWithId()} add insets listener -> ${listeners.size.inc()}" }
        val key = nextKey++
        // listeners may or may not be notified with new insets
        srcState = srcState.copy(hasListeners = true, dependencies = listeners.dependencies(listener))
        listeners[key] = listener
        // always notify the new one
        listener.onApplyWindowInsets(current)
        return key
    }

    override fun removeInsetsListener(listener: InsetsListener) {
        val entry = listeners.entries.find { it.value === listener }
        entry ?: return logd { "${thisView?.nameWithId()} listener not found" }
        removeInsetsListener(entry.key)
    }

    override fun removeInsetsListener(key: Int) {
        val removed = listeners.remove(key) != null
        logd { "${thisView?.nameWithId()} remove insets listener? $removed -> ${listeners.size}" }
        if (removed) {
            srcState = srcState.copy(hasListeners = listeners.isNotEmpty(), dependencies = listeners.dependencies())
        }
    }

    // the one of the two entry points for system window insets
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (provider == null) {
            val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, thisView)
            srcState = srcState.copy(windowInsets = windowInsetsCompat.toExtended())
        }
        return windowInsets
    }

    override fun requestInsets() = updateCurrent(srcState)

    // the one of the two entry points for system window insets
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        srcState = srcState.copy(windowInsets = windowInsets)
    }

    private fun updateCurrent(srcState: SrcState) {
        logd { "${thisView?.nameWithId()} update current with modifier? ${insetsModifier != null}" }
        current = insetsModifier
            ?.transform(listeners.isNotEmpty(), srcState.windowInsets)
            ?.toExtended()
            ?: srcState.windowInsets
    }

    private fun notifyListeners(windowInsets: ExtendedWindowInsets) {
        listeners.values.toTypedArray().forEach {
            it.onApplyWindowInsets(windowInsets)
        }
    }

    private fun InsetsListener.checkTheSameView(other: InsetsListener) = when {
        this !is ViewInsetsDelegateImpl -> Unit
        other !is ViewInsetsDelegateImpl -> Unit
        other.view !== view -> Unit
        else -> throw MultipleViewInsetsDelegate("The target view ${view.nameWithId()}")
    }
}

private fun Map<Int, InsetsListener>.dependencies(another: InsetsListener? = null): Int {
    return count { it.value.dependency } + if (another?.dependency == true) 1 else 0
}
