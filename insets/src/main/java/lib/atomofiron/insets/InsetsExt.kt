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
import android.view.ViewParent
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import lib.atomofiron.insets.InsetsDestination.*


val barsWithCutout: Int = Type.systemBars() or Type.displayCutout()

fun WindowInsetsCompat.displayCutout(): Insets = getInsets(Type.displayCutout())

fun WindowInsetsCompat.systemBars(): Insets = getInsets(Type.systemBars())

fun WindowInsetsCompat.barsWithCutout(): Insets = getInsets(barsWithCutout)

fun WindowInsetsCompat.isEmpty(typeMask: Int): Boolean = getInsets(typeMask).isEmpty()

fun Insets.isEmpty() = this == Insets.NONE

fun ViewParent.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.addInsetsListener(listener: InsetsListener): Int {
    val provider = (this as? InsetsProvider) ?: parent.findInsetsProvider()
    val key = provider?.addInsetsListener(listener)
    key ?: logd { "${nameWithId()} insets provider not found" }
    return key ?: INVALID_INSETS_LISTENER_KEY
}

fun View.syncInsets(
    typeMask: Int = barsWithCutout,
    dependency: Boolean = false,
    block: (ViewInsetsConfig.() -> Unit)? = null,
): ViewInsetsDelegate {
    return block?.let {
        val config = ViewInsetsConfigImpl().apply(it)
        ViewInsetsDelegateImpl(this, dependency, typeMask, config.dstStart, config.dstTop, config.dstEnd, config.dstBottom)
    } ?: ViewInsetsDelegateImpl(this, dependency, typeMask)
}

fun ViewInsetsDelegate.padding(top: Boolean = false, horizontal: Boolean = false, bottom: Boolean = false)
    = padding(start = horizontal, top = top, end = horizontal, bottom = bottom)

fun ViewInsetsDelegate.margin(top: Boolean = false, horizontal: Boolean = false, bottom: Boolean = false)
    = margin(start = horizontal, top = top, end = horizontal, bottom = bottom)

fun ViewInsetsDelegate.padding(horizontal: Boolean = false, vertical: Boolean = false)
    = padding(start = horizontal, top = vertical, end = horizontal, bottom = vertical)

fun ViewInsetsDelegate.margin(horizontal: Boolean = false, vertical: Boolean = false)
    = margin(start = horizontal, top = vertical, end = horizontal, bottom = vertical)

fun ViewInsetsDelegate.padding(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false)
    = applyInsets(
        start = Padding.takeIf { start },
        top = Padding.takeIf { top },
        end = Padding.takeIf { end },
        bottom = Padding.takeIf { bottom },
    )

fun ViewInsetsDelegate.margin(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false)
    = applyInsets(
        start = Margin.takeIf { start },
        top = Margin.takeIf { top },
        end = Margin.takeIf { end },
        bottom = Margin.takeIf { bottom },
    )

inline fun InsetsProvider.composeInsets(
    // these receive original insets (from parent provider or stock system window insets)
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, ExtendedWindowInsets) -> WindowInsetsCompat,
) {
    delegates.forEach { it.unsubscribeInsets() }
    setInsetsModifier { hasListeners, windowInsets ->
        delegates.forEach { it.onApplyWindowInsets(windowInsets) }
        transformation(hasListeners, windowInsets).toExtended()
    }
}

fun WindowInsetsCompat.toExtended() = when (this) {
    is ExtendedWindowInsets -> this
    else -> ExtendedWindowInsets(this)
}

inline fun <T : ExtendedWindowInsets.Type> T.from(
    windowInsets: WindowInsetsCompat,
    block: T.() -> Int,
): Insets {
    return windowInsets.getInsets(block())
}

inline operator fun <T : ExtendedWindowInsets.Type> WindowInsetsCompat.invoke(
    companion: T,
    block: T.() -> Int,
): Insets {
    return getInsets(companion.block())
}

operator fun WindowInsetsCompat.get(type: Int): Insets = getInsets(type)

fun View.onAttachCallback(
    onAttach: (View) -> Unit,
    onDetach: (View) -> Unit,
): View.OnAttachStateChangeListener {
    if (isAttachedToWindow) onAttach(this)
    val ex = Exception()
    return object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            try {
                onAttach(view)
            } catch (e: java.lang.Exception) {
                ex.printStackTrace()
                throw e
            }
        }
        override fun onViewDetachedFromWindow(view: View) = onDetach(view)
    }.also { addOnAttachStateChangeListener(it) }
}
