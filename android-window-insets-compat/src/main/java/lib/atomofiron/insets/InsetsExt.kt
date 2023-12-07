package lib.atomofiron.insets

import android.view.View
import android.view.ViewParent
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type


val barsWithCutout: Int = Type.systemBars() or Type.displayCutout()

fun WindowInsetsCompat.cutout(): Insets = getInsets(Type.displayCutout())

fun WindowInsetsCompat.systemBars(): Insets = getInsets(Type.systemBars())

fun WindowInsetsCompat.barsWithCutout(): Insets = getInsets(barsWithCutout)

fun WindowInsetsCompat.isEmpty(typeMask: Int): Boolean = getInsets(typeMask).isEmpty()

fun Insets.isEmpty() = this == Insets.NONE

fun ViewParent.getInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.getInsetsProvider()
}

fun View.syncInsetsPadding() = syncInsetsPadding(this.parent)

fun View.syncInsetsPadding(parent: ViewParent = this.parent) = syncInsetsPadding(parent, start = true, top = true, end = true, bottom = true)

fun View.syncInsetsPadding(
    parent: ViewParent = this.parent,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
) {
    val provider = parent.getInsetsProvider() ?: return poop("null for ${resources.getResourceEntryName(id)}")
    ViewInsetsDelegateImpl(this, start, top, end, bottom, InsetsDestination.Padding, provider)
}

fun View.syncInsetsMargin() = syncInsetsMargin(this.parent)

fun View.syncInsetsMargin(parent: ViewParent = this.parent) = syncInsetsMargin(parent, start = true, top = true, end = true, bottom = true)

fun View.syncInsetsMargin(
    parent: ViewParent = this.parent,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
) {
    val provider = parent.getInsetsProvider() ?: return poop("null for ${resources.getResourceEntryName(id)}")
    ViewInsetsDelegateImpl(this, start, top, end, bottom, InsetsDestination.Margin, provider)
}

fun View.insetsPaddingDelegate(
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
): ViewInsetsDelegate = ViewInsetsDelegateImpl(this, start, top, end, bottom, InsetsDestination.Padding, provider = null)

fun View.insetsMarginDelegate(
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
): ViewInsetsDelegate = ViewInsetsDelegateImpl(this, start, top, end, bottom, InsetsDestination.Margin, provider = null)


inline fun InsetsProvider.composeInsets(
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, WindowInsetsCompat) -> WindowInsetsCompat,
) {
    setInsetsModifier { hasListeners, windowInsets ->
        delegates.forEach { it.apply(windowInsets) }
        transformation(hasListeners, windowInsets)
    }
}
