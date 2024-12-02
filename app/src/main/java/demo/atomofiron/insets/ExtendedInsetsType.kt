package demo.atomofiron.insets

import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.TypeSet

object ExtType : ExtendedWindowInsets.Type() {
    val togglePanel = define("togglePanel")
    val verticalPanels = define("verticalPanels")
    val fab = define("fab")
    val general = define("general")

    inline operator fun invoke(block: ExtType.() -> TypeSet): TypeSet = ExtType.block()
}

// associate your custom type with ExtendedWindowInsets
operator fun ExtendedWindowInsets.invoke(block: ExtType.() -> TypeSet): Insets = get(ExtType.block())
