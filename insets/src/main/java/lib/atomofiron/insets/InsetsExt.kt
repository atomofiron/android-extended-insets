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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.barsWithCutout
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding


val insetsCombining = InsetsCombining(combiningTypeMask = Type.displayCutout())

fun WindowInsetsCompat.displayCutout(): Insets = getInsets(Type.displayCutout())

fun WindowInsetsCompat.systemBars(): Insets = getInsets(Type.systemBars())

fun WindowInsetsCompat.barsWithCutout(): Insets = getInsets(barsWithCutout)

fun WindowInsetsCompat.isEmpty(typeMask: Int): Boolean = getInsets(typeMask).isEmpty()

fun WindowInsetsCompat.isNotEmpty(typeMask: Int): Boolean = !isEmpty(typeMask)

fun Insets.isEmpty() = this == Insets.NONE

fun Insets.isNotEmpty() = !isEmpty()

fun ViewParent.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.addInsetsListener(listener: InsetsListener): Int {
    val provider = (this as? InsetsProvider) ?: parent.findInsetsProvider()
    val key = provider?.addInsetsListener(listener)
    key ?: logd { "${nameWithId()} unable add insets listener, provider not found" }
    return key ?: INVALID_INSETS_LISTENER_KEY
}

fun View.removeInsetsListener(listener: InsetsListener) {
    val provider = (this as? InsetsProvider) ?: parent.findInsetsProvider()
    provider?.removeInsetsListener(listener)
        ?: logd { "${nameWithId()} unable remove insets listener, provider not found" }
}

fun View.insetsMix(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    block: (ViewInsetsConfig.() -> Unit)? = null,
): ViewInsetsDelegate {
    return block?.let {
        ViewInsetsConfig().apply(it).run {
            ViewInsetsDelegateImpl(this@insetsMix, typeMask, combining, dstStart, dstTop, dstEnd, dstBottom)
        }
    } ?: ViewInsetsDelegateImpl(this, typeMask)
}

fun View.insetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
): ViewInsetsDelegate = ViewInsetsDelegateImpl(
    this,
    typeMask,
    combining,
    if (start) Padding else None,
    if (top) Padding else None,
    if (end) Padding else None,
    if (bottom) Padding else None,
)

fun View.insetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
): ViewInsetsDelegate = ViewInsetsDelegateImpl(
    this,
    typeMask,
    combining,
    if (start) Margin else None,
    if (top) Margin else None,
    if (end) Margin else None,
    if (bottom) Margin else None,
)

fun View.insetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = insetsPadding(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.insetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = insetsMargin(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.insetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = insetsPadding(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.insetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = insetsMargin(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.insetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    dependency: Boolean = false,
) = insetsMargin(typeMask, combining, horizontal = true, vertical = true)

fun View.insetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    dependency: Boolean = false,
) = insetsPadding(typeMask, combining, horizontal = true, vertical = true)

inline fun InsetsProvider.composeInsets(
    // these delegates receive original insets (from parent provider or stock system window insets)
    delegate: ViewInsetsDelegate,
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, ExtendedWindowInsets) -> WindowInsetsCompat,
) {
    delegate.detachFromProvider()
    delegates.forEach { it.detachFromProvider() }
    setInsetsModifier { hasListeners, windowInsets ->
        delegate.onApplyWindowInsets(windowInsets)
        delegates.forEach { it.onApplyWindowInsets(windowInsets) }
        transformation(hasListeners, windowInsets).toExtended()
    }
}

fun View.getWindowInsets(): WindowInsetsCompat {
    return (this as? InsetsProvider)?.current
        ?: parent.findInsetsProvider()?.current
        ?: ViewCompat.getRootWindowInsets(this)
        ?: WindowInsetsCompat.CONSUMED
}

fun View.getInsets(typeMask: Int = barsWithCutout): Insets = getWindowInsets().getInsets(typeMask)

fun WindowInsetsCompat.toExtended() = when (this) {
    is ExtendedWindowInsets -> this
    else -> ExtendedWindowInsets(this)
}

inline fun <T : ExtendedWindowInsets.Type> T.from(
    windowInsets: WindowInsetsCompat,
    block: T.() -> Int,
): Insets = windowInsets.getInsets(block())

inline operator fun <T : ExtendedWindowInsets.Type> WindowInsetsCompat.invoke(
    companion: T,
    block: T.() -> Int,
): Insets = getInsets(companion.block())

operator fun WindowInsetsCompat.get(type: Int): Insets = getInsets(type)

fun InsetsProvider.requestInsetOnLayoutChange(
    horizontally: Boolean = false,
    vertically: Boolean = false,
    vararg views: View,
) {
    val listener = View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (horizontally && (left != oldLeft || right != oldRight) || vertically && (top != oldTop || bottom != oldBottom)) {
            requestInsets()
        }
    }
    for (view in views) view.addOnLayoutChangeListener(listener)
}
