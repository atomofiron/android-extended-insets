package demo.atomofiron.insets

import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets

object ExtType : ExtendedWindowInsets.Type() {
    val verticalPanels = next()
    val togglePanel = next()
    val all = verticalPanels or togglePanel or systemBars or displayCutout
}

// associate your custom type with ExtendedWindowInsets
operator fun ExtendedWindowInsets.invoke(block: ExtType.() -> Int): Insets = get(ExtType.block())
