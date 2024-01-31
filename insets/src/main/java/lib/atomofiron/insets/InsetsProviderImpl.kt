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
import androidx.core.view.WindowInsetsCompat.Type as CompatType
import lib.atomofiron.insets.ExtendedWindowInsets.Type


const val INVALID_INSETS_LISTENER_KEY = 0

class InsetsProviderImpl private constructor(
    private var dropNative: Boolean,
) : InsetsProvider, View.OnAttachStateChangeListener {

    private var source = ExtendedWindowInsets.CONSUMED
        set(value) {
            logd { "${thisView?.nameWithId()} new received? ${field != value}" }
            if (field != value) {
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
    private var isRequested = false
    private var isNotifying = false

    constructor() : this(dropNative = false)

    constructor(context: Context, attrs: AttributeSet?) : this(context.dropNativeInsets(attrs))

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
        updateCurrent(source)
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
        logd { "${thisView?.nameWithId()} add listener -> ${listeners.size.inc()}" }
        val key = nextKey++
        listeners[key] = listener
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
        logd { "${thisView?.nameWithId()} remove listener? $removed -> ${listeners.size}" }
    }

    // the one of the two entry points for window insets
    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (provider == null) {
            val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, thisView)
            val barsWithCutout = windowInsetsCompat.getInsets(CompatType.systemBars() or CompatType.displayCutout())
            source = ExtendedWindowInsets.Builder(windowInsetsCompat)
                .set(Type.general, barsWithCutout)
                .build()
        }
        return if (dropNative) WindowInsets.CONSUMED else windowInsets
    }

    override fun requestInsets() = when {
        isNotifying -> isRequested = true
        else -> updateCurrent(source)
    }

    override fun dropNativeInsets(drop: Boolean) {
        dropNative = drop
    }

    // the one of the two entry points for window insets
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        source = windowInsets
    }

    private fun updateCurrent(source: ExtendedWindowInsets) {
        logd { "${thisView?.nameWithId()} update current, with modifier? ${insetsModifier != null}" }
        current = insetsModifier
            ?.transform(listeners.isNotEmpty(), source)
            ?.toExtended()
            ?: source
    }

    private fun notifyListeners(windowInsets: ExtendedWindowInsets) {
        isNotifying = true
        listeners.values.toTypedArray().forEach {
            it.onApplyWindowInsets(windowInsets)
        }
        if (isRequested) {
            isRequested = false
            updateCurrent(source)
        }
        isNotifying = false
    }
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
