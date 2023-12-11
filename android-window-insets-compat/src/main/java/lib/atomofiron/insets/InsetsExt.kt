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

fun ViewParent.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.syncInsets(typeMask: Int = barsWithCutout): ViewInsetsDelegate = ViewInsetsDelegateImpl(this, typeMask)

inline fun InsetsProvider.composeInsets(
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, WindowInsetsCompat) -> WindowInsetsCompat,
) {
    delegates.forEach { it.detachInsetsProvider() }
    setInsetsModifier { hasListeners, windowInsets ->
        delegates.forEach { it.onApplyWindowInsets(windowInsets) }
        transformation(hasListeners, windowInsets)
    }
}
