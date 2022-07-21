/*
 * Copyright 2022 Yaroslav Nesterov
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

package lib.atomofiron.android_window_insets_compat

internal sealed class Destination(
    val applyStart: Boolean,
    val applyTop: Boolean,
    val applyEnd: Boolean,
    val applyBottom: Boolean,
) {
    val isEmpty = !applyStart && !applyTop && !applyEnd && !applyBottom
    val isNotEmpty = !isEmpty

    class Padding(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean) : Destination(start, top, end, bottom)
    class Margin(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean) : Destination(start, top, end, bottom)
}