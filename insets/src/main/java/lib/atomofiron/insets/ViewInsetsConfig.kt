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

import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None

class ViewInsetsConfig {

    enum class Side {
        Start, Top, End, Bottom, Horizontal, Vertical
    }

    val padding = Padding
    val margin = Margin

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

    fun padding(vararg sides: Side) = set(Padding, *sides)

    fun margin(vararg sides: Side) = set(Margin, *sides)

    private fun set(destination: InsetsDestination, vararg sides: Side): ViewInsetsConfig {
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

    fun set(
        start: InsetsDestination = dstStart,
        top: InsetsDestination = dstTop,
        end: InsetsDestination = dstEnd,
        bottom: InsetsDestination = dstBottom,
    ): ViewInsetsConfig {
        dstStart = start
        dstTop = top
        dstEnd = end
        dstBottom = bottom
        return this
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
}