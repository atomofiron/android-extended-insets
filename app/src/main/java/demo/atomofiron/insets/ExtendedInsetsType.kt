package demo.atomofiron.insets

import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.TypeSet

object ExtType : ExtendedWindowInsets.Type() {
    val togglePanel = next("togglePanel")
    val verticalPanels = next("verticalPanels")
    val fabTop = next("fabTop")
    val fabHorizontal = next("fabHorizontal")
}

// associate your custom type with ExtendedWindowInsets
operator fun ExtendedWindowInsets.invoke(block: ExtType.() -> TypeSet): Insets = get(ExtType.block())
