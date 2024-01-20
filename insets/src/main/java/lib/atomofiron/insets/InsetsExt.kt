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
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding


val barsWithCutout: Int = Type.systemBars() or Type.displayCutout()

val insetsCombining = InsetsCombining(combiningTypeMask = Type.displayCutout())

fun WindowInsetsCompat.displayCutout(): Insets = getInsets(Type.displayCutout())

fun WindowInsetsCompat.systemBars(): Insets = getInsets(Type.systemBars())

fun WindowInsetsCompat.barsWithCutout(): Insets = getInsets(barsWithCutout)

fun WindowInsetsCompat.isEmpty(typeMask: Int): Boolean = getInsets(typeMask).isEmpty()

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

fun View.withInsets(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    block: (ViewInsetsConfig.() -> Unit)? = null,
): ViewInsetsDelegate {
    return block?.let {
        ViewInsetsConfig().apply(it).run {
            ViewInsetsDelegateImpl(this@withInsets, typeMask, combining, dstStart, dstTop, dstEnd, dstBottom)
        }
    } ?: ViewInsetsDelegateImpl(this, typeMask)
}

fun View.withInsetsPadding(
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

fun View.withInsetsMargin(
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

fun View.withInsetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = withInsetsPadding(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.withInsetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = withInsetsMargin(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.withInsetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = withInsetsPadding(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.withInsetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = withInsetsMargin(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.withInsetsMargin(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    dependency: Boolean = false,
) = withInsetsMargin(typeMask, combining, horizontal = true, vertical = true)

fun View.withInsetsPadding(
    typeMask: Int = barsWithCutout,
    combining: InsetsCombining? = null,
    dependency: Boolean = false,
) = withInsetsPadding(typeMask, combining, horizontal = true, vertical = true)

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
