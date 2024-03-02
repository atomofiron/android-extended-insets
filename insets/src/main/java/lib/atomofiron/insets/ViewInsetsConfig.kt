/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Translation
import lib.atomofiron.insets.ViewInsetsConfig.Side

class ViewInsetsConfig : IViewInsetsConfig {

    enum class Side {
        Start, Top, End, Bottom, Horizontal, Vertical
    }

    val padding = Padding
    val margin = Margin
    val translation = Translation

    val start = Side.Start
    val top = Side.Top
    val end = Side.End
    val bottom = Side.Bottom
    val horizontal = Side.Horizontal
    val vertical = Side.Vertical

    internal var dstStart = None
        private set
    internal var dstTop = None
        private set
    internal var dstEnd = None
        private set
    internal var dstBottom = None
        private set

    override fun padding(vararg sides: Side) = set(Padding, *sides)

    override fun margin(vararg sides: Side) = set(Margin, *sides)

    override fun translation(vararg sides: Side) = set(Translation, *sides)

    private fun set(destination: InsetsDestination, vararg sides: Side): IViewInsetsConfig {
        sides.forEach {
            when (it) {
                Side.Start -> dstStart = destination
                Side.Top -> dstTop = destination
                Side.End, -> dstEnd = destination
                Side.Bottom -> dstBottom = destination
                Side.Horizontal -> {
                    dstStart = destination
                    dstEnd = destination
                }
                Side.Vertical -> {
                    dstTop = destination
                    dstBottom = destination
                }
            }
        }
        return this
    }

    override fun set(
        start: InsetsDestination?,
        top: InsetsDestination?,
        end: InsetsDestination?,
        bottom: InsetsDestination?,
    ): IViewInsetsConfig {
        dstStart = start ?: dstStart
        dstTop = top ?: dstTop
        dstEnd = end ?: dstEnd
        dstBottom = bottom ?: dstBottom
        return this
    }

    override fun start(destination: InsetsDestination): IViewInsetsConfig {
        dstStart = destination
        return this
    }

    override fun top(destination: InsetsDestination): IViewInsetsConfig {
        dstTop = destination
        return this
    }

    override fun end(destination: InsetsDestination): IViewInsetsConfig {
        dstEnd = destination
        return this
    }

    override fun bottom(destination: InsetsDestination): IViewInsetsConfig {
        dstBottom = destination
        return this
    }

    override fun horizontal(destination: InsetsDestination): IViewInsetsConfig {
        dstStart = destination
        dstEnd = destination
        return this
    }

    override fun vertical(destination: InsetsDestination): IViewInsetsConfig {
        dstTop = destination
        dstBottom = destination
        return this
    }
}

interface IViewInsetsConfig {
    fun padding(vararg sides: Side): IViewInsetsConfig
    fun margin(vararg sides: Side): IViewInsetsConfig
    fun translation(vararg sides: Side): IViewInsetsConfig
    fun start(destination: InsetsDestination): IViewInsetsConfig
    fun top(destination: InsetsDestination): IViewInsetsConfig
    fun end(destination: InsetsDestination): IViewInsetsConfig
    fun bottom(destination: InsetsDestination): IViewInsetsConfig
    fun horizontal(destination: InsetsDestination): IViewInsetsConfig
    fun vertical(destination: InsetsDestination): IViewInsetsConfig
    fun set(
        start: InsetsDestination? = null,
        top: InsetsDestination? = null,
        end: InsetsDestination? = null,
        bottom: InsetsDestination? = null,
    ): IViewInsetsConfig
}
