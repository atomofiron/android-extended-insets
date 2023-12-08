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

fun View.syncInsets(): ViewInsetsDelegate = ViewInsetsDelegateImpl(this)

inline fun InsetsProvider.composeInsets(
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, WindowInsetsCompat) -> WindowInsetsCompat,
) {
    delegates.forEach { it.detach() }
    setInsetsModifier { hasListeners, windowInsets ->
        delegates.forEach { it.apply(windowInsets) }
        transformation(hasListeners, windowInsets)
    }
}
