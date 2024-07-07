/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Translation

class ViewInsetsConfig(block: ViewInsetsConfig.() -> Unit) {

    enum class Side {
        Start, Top, End, Bottom, Horizontal, Vertical
    }

    val start = Side.Start
    val top = Side.Top
    val end = Side.End
    val bottom = Side.Bottom
    val horizontal = Side.Horizontal
    val vertical = Side.Vertical

    val padding = Padding
    val margin = Margin
    val translation = Translation

    internal var dstStart = None
        private set
    internal var dstTop = None
        private set
    internal var dstEnd = None
        private set
    internal var dstBottom = None
        private set

    init {
        block()
    }

    fun start(destination: InsetsDestination): ViewInsetsConfig {
        dstStart = destination
        return this
    }

    fun top(destination: InsetsDestination): ViewInsetsConfig {
        dstTop = destination
        return this
    }

    fun end(destination: InsetsDestination): ViewInsetsConfig {
        dstEnd = destination
        return this
    }

    fun bottom(destination: InsetsDestination): ViewInsetsConfig {
        dstBottom = destination
        return this
    }

    fun horizontal(destination: InsetsDestination): ViewInsetsConfig {
        dstStart = destination
        dstEnd = destination
        return this
    }

    fun vertical(destination: InsetsDestination): ViewInsetsConfig {
        dstTop = destination
        dstBottom = destination
        return this
    }

    fun padding(vararg sides: Side) = set(Padding, *sides)

    fun margin(vararg sides: Side) = set(Margin, *sides)

    fun translation(vararg sides: Side) = set(Translation, *sides)

    private fun set(destination: InsetsDestination, vararg sides: Side): ViewInsetsConfig {
        sides.forEach {
            when (it) {
                Side.Start -> start(destination)
                Side.Top -> top(destination)
                Side.End, -> end(destination)
                Side.Bottom -> bottom(destination)
                Side.Horizontal -> horizontal(destination)
                Side.Vertical -> vertical(destination)
            }
        }
        return this
    }
}
