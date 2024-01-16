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

internal class ViewInsetsConfigImpl : ViewInsetsConfig {

    var dstStart = None
        private set
    var dstTop = None
        private set
    var dstEnd = None
        private set
    var dstBottom = None
        private set

    override fun padding(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsConfig {
        dstStart = if (start) Padding else dstStart
        dstTop = if (top) Padding else dstTop
        dstEnd = if (end) Padding else dstEnd
        dstBottom = if (bottom) Padding else dstBottom
        return this
    }

    override fun margin(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsConfig {
        dstStart = if (start) Margin else dstStart
        dstTop = if (top) Margin else dstTop
        dstEnd = if (end) Margin else dstEnd
        dstBottom = if (bottom) Margin else dstBottom
        return this
    }
}