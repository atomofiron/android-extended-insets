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
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.barsWithCutout
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding


val insetsCombining by lazy(LazyThreadSafetyMode.NONE) {
    InsetsCombining(combiningTypes = ExtendedWindowInsets.Type.displayCutout)
}

fun ExtendedWindowInsets.isEmpty(types: TypeSet): Boolean = get(types).isEmpty()

fun ExtendedWindowInsets.isNotEmpty(types: TypeSet): Boolean = !isEmpty(types)

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
    typeMask: TypeSet = barsWithCutout,
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
    typeMask: TypeSet = barsWithCutout,
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
    typeMask: TypeSet = barsWithCutout,
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
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = insetsPadding(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.insetsMargin(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
    top: Boolean = false,
    horizontal: Boolean = false,
    bottom: Boolean = false,
) = insetsMargin(typeMask, combining, horizontal, top, horizontal, bottom)

fun View.insetsPadding(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = insetsPadding(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.insetsMargin(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
    horizontal: Boolean = false,
    vertical: Boolean = false,
) = insetsMargin(typeMask, combining, horizontal, vertical, horizontal, vertical)

fun View.insetsMargin(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
) = insetsMargin(typeMask, combining, horizontal = true, vertical = true)

fun View.insetsPadding(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
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

fun View.getInsets(type: Int = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()): Insets
    = getWindowInsets().getInsets(type)

fun View.getInsets(type: TypeSet = barsWithCutout): Insets = when (val windowInsets = getWindowInsets()) {
    is ExtendedWindowInsets -> windowInsets[type]
    else -> {
        var typeMask = 0
        type.forEach {
            if (it.seed in LEGACY_RANGE)
                typeMask = typeMask or it.seed.toTypeMask()
        }
        windowInsets.getInsets(typeMask)
    }
}

operator fun WindowInsetsCompat.get(typeMask: Int): Insets = getInsets(typeMask)

fun WindowInsetsCompat.toExtended() = when (this) {
    is ExtendedWindowInsets -> this
    else -> ExtendedWindowInsets(this)
}

inline fun <T : ExtendedWindowInsets.Type> T.from(
    windowInsets: ExtendedWindowInsets,
    block: T.() -> TypeSet,
): Insets = windowInsets[block()]

inline operator fun <T : ExtendedWindowInsets.Type> ExtendedWindowInsets.invoke(
    companion: T,
    block: T.() -> TypeSet,
): Insets = get(companion.block())

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

internal fun Int.toTypeMask(): Int = when {
    this > LEGACY_LIMIT -> throw IllegalArgumentException("Seed $this is too big to be converted to the legacy bit mask, $LEGACY_LIMIT is the limit")
    else -> 1.shl(dec())
}

internal fun Int.toTypeSet(name: String? = null): TypeSet {
    if (this == 0) {
        return TypeSet.EMPTY
    }
    var cursor = 1
    var seed = TypeSet.FIRST_SEED
    var head: TypeSet? = null
    while (cursor <= this || this < 0 && cursor != 0) {
        if ((cursor and this) != 0) {
            head = name
                ?.let { TypeSet(it, seed, head) }
                ?: Type.types.find { it.seed == seed }
                ?: TypeSet("", seed, head)
        }
        cursor = cursor.shl(1)
        seed++
    }
    return head!!
}
