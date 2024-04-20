/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewParent
import androidx.annotation.LayoutRes
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type as CompatType
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.barsWithCutout
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Translation


val insetsCombining by lazy(LazyThreadSafetyMode.NONE) {
    InsetsCombining(combiningTypes = Type.displayCutout)
}

fun Insets.isEmpty() = this == Insets.NONE

fun Insets.isNotEmpty() = !isEmpty()

fun Insets.inv() = Insets.of(-left, -top, -right, -bottom)

fun Insets.ltrb() = when (this) {
    MAX_INSETS -> "full"
    else -> "${left.orMax()},${top.orMax()},${right.orMax()},${bottom.orMax()}"
}

private fun Int.orMax(): String = if (this == MAX_INSET) "max" else toString()

val MAX_INSETS = Insets.of(MAX_INSET, MAX_INSET, MAX_INSET, MAX_INSET)

fun Activity.setContentView(@LayoutRes layoutId: Int, insetsProvider: InsetsProvider): InsetsProviderFrameLayout {
    val layout = InsetsProviderFrameLayout(this, insetsProvider)
    setContentView(layout)
    LayoutInflater.from(this).inflate(layoutId, layout)
    return layout
}

fun Activity.setContentView(view: View, insetsProvider: InsetsProvider): InsetsProviderFrameLayout {
    val layout = InsetsProviderFrameLayout(this, insetsProvider)
    setContentView(layout)
    layout.addView(view)
    return layout
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

fun View.insetsTranslation(
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
    if (start) Translation else None,
    if (top) Translation else None,
    if (end) Translation else None,
    if (bottom) Translation else None,
)

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

fun View.insetsSource(callback: ViewInsetsSourceCallback) = insetsSource(horizontal = true, vertical = true, callback)

fun View.insetsSource(
    horizontal: Boolean = false,
    vertical: Boolean = false,
    callback: ViewInsetsSourceCallback,
): ViewInsetsSource = ViewInsetsSourceImpl(this, horizontal, vertical, callback)

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

internal fun Int.toTypeMask(): Int = when {
    this > LEGACY_LIMIT -> throw IllegalArgumentException("Seed $this is too big to be converted to the legacy bit mask, $LEGACY_LIMIT is the limit")
    else -> 1.shl(dec())
}

internal fun TypeSet.toLegacyType(): Int = seed.toTypeMask()

internal fun Int.toTypeSet(name: String? = null): TypeSet {
    if (this == 0) {
        return TypeSet.Empty
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
        if (next.isNotEmpty() || !isVisible(typeMask))
            values[seed] = next.toValues()
    }
    return values
}

internal fun WindowInsetsCompat?.getHidden(): TypeSet {
    var hidden = TypeSet.Empty
    this ?: return hidden
    for (seed in LEGACY_RANGE) {
        val typeMask = seed.toTypeMask()
        if (!isVisible(typeMask)) {
            hidden += typeMask.toTypeSet()
        }
    }
    return hidden
}
