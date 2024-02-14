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
import androidx.core.view.isGone
import androidx.core.view.WindowInsetsCompat.Type as CompatType
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.barsWithCutout
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding


val insetsCombining by lazy(LazyThreadSafetyMode.NONE) {
    InsetsCombining(combiningTypes = Type.displayCutout)
}

fun ExtendedWindowInsets.isEmpty(types: TypeSet): Boolean = get(types).isEmpty()

fun ExtendedWindowInsets.isNotEmpty(types: TypeSet): Boolean = !isEmpty(types)

fun Insets.isEmpty() = this == Insets.NONE

fun Insets.isNotEmpty() = !isEmpty()

fun Insets.inv() = Insets.of(-left, -top, -right, -bottom)

fun Insets.ltrb() = "$left,$top,$right,$bottom"

val MAX_INSETS = Insets.of(MAX_INSET, MAX_INSET, MAX_INSET, MAX_INSET)

fun Insets.consume(other: Insets): Insets = when {
    isEmpty() -> this
    other.isEmpty() -> this
    else -> Insets.of(
        (left - other.left).coerceAtLeast(0),
        (top - other.top).coerceAtLeast(0),
        (right - other.right).coerceAtLeast(0),
        (bottom - other.bottom).coerceAtLeast(0),
    )
}

fun ViewParent.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.requestInsets() {
    findInsetsProvider()?.requestInsets()
}

fun View.addInsetsListener(listener: InsetsListener): Int {
    val key = findInsetsProvider()?.addInsetsListener(listener)
    key ?: logd { "${nameWithId()} unable add insets listener, provider not found" }
    return key ?: INVALID_INSETS_LISTENER_KEY
}

fun View.removeInsetsListener(listener: InsetsListener) {
    findInsetsProvider()?.removeInsetsListener(listener)
        ?: logd { "${nameWithId()} unable remove insets listener, provider not found" }
}

fun View.insetsMix(
    typeMask: TypeSet = barsWithCutout,
    combining: InsetsCombining? = null,
    config: (ViewInsetsConfig.() -> Unit)? = null,
): ViewInsetsDelegate {
    return config?.let {
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

fun ExtendedWindowInsets.builder(): ExtendedBuilder = ExtendedWindowInsets.Builder(this)

fun View.getWindowInsets(): ExtendedWindowInsets {
    return findInsetsProvider()?.current ?: ExtendedWindowInsets(ViewCompat.getRootWindowInsets(this))
}

fun View.getInsets(type: Int = CompatType.systemBars() or CompatType.displayCutout()): Insets
    = getWindowInsets()[type.toTypeSet()]

fun View.getInsets(type: TypeSet = barsWithCutout): Insets = getWindowInsets()[type]

operator fun WindowInsetsCompat.get(typeMask: Int): Insets = getInsets(typeMask)

inline fun <T : Type> T.from(
    windowInsets: ExtendedWindowInsets,
    block: T.() -> TypeSet,
): Insets = windowInsets[block()]

inline operator fun <T : Type> ExtendedWindowInsets.invoke(
    companion: T,
    block: T.() -> TypeSet,
): Insets = get(companion.block())

fun View.onAttachCallback(
    onAttach: ((View) -> Unit)? = null,
    onDetach: ((View) -> Unit)? = null,
): View.OnAttachStateChangeListener {
    if (isAttachedToWindow) onAttach?.invoke(this)
    return object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) { onAttach?.invoke(view) }
        override fun onViewDetachedFromWindow(view: View) { onDetach?.invoke(view) }
    }.also { addOnAttachStateChangeListener(it) }
}

fun requestInsetsOnVisibilityChange(vararg views: View) {
    val placements = views.map { !it.isGone }.toMutableList()
    val visibilityChecker = {
        val providers = mutableListOf<InsetsProvider>()
        views.forEachIndexed { index, view ->
            val placed = !view.isGone
            if (placed != placements[index]) {
                placements[index] = placed
                if (placed) {
                    view.findInsetsProvider()
                        ?.takeIf { provider -> providers.find { it === provider } == null }
                        ?.also { providers.add(it) }
                        ?.requestInsets()
                }
            }
        }
    }
    views.forEach { view ->
        view.onAttachCallback(
            onAttach = { view.viewTreeObserver.addOnGlobalLayoutListener(visibilityChecker) },
            onDetach = { view.viewTreeObserver.removeOnGlobalLayoutListener(visibilityChecker) },
        )
    }
}

fun requestInsetsOnLayoutChange(vararg views: View)
    = requestInsetsOnLayoutChange(*views, horizontally = true, vertically = true)

fun requestInsetsOnLayoutChange(
    vararg views: View,
    horizontally: Boolean = false,
    vertically: Boolean = false,
) {
    val listener = View.OnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (horizontally && (left != oldLeft || right != oldRight) || vertically && (top != oldTop || bottom != oldBottom)) {
            view.requestInsets()
        }
    }
    for (view in views) view.addOnLayoutChangeListener(listener)
}

internal fun Int.toTypeMask(): Int = when {
    this > LEGACY_LIMIT -> throw IllegalArgumentException("Seed $this is too big to be converted to the legacy bit mask, $LEGACY_LIMIT is the limit")
    else -> 1.shl(dec())
}

internal fun TypeSet.toLegacyType(): Int = seed.toTypeMask()

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
                ?: TypeSet(NAME_UNDEFINED, seed, head)
        }
        cursor = cursor.shl(1)
        seed++
    }
    return head!!
}

internal fun WindowInsetsCompat?.getValues(): Map<Int, InsetsValue> {
    val values = mutableMapOf<Int, InsetsValue>()
    this ?: return values
    for (seed in LEGACY_RANGE) {
        val typeMask = seed.toTypeMask()
        val next = when (typeMask) {
            CompatType.ime() -> getInsets(typeMask)
            else -> getInsetsIgnoringVisibility(typeMask)
        }
        if (next.isNotEmpty() || isVisible(typeMask))
            values[seed] = next.toValues()
    }
    return values
}

internal fun WindowInsetsCompat?.getHidden(): TypeSet {
    var hidden = TypeSet.EMPTY
    this ?: return hidden
    for (seed in LEGACY_RANGE) {
        val typeMask = seed.toTypeMask()
        if (!isVisible(typeMask)) {
            hidden += typeMask.toTypeSet()
        }
    }
    return hidden
}
