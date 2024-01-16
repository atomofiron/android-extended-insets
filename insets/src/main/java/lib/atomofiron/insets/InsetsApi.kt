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

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat

fun interface InsetsListener {
    fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets)
}

fun interface InsetsModifier {
    fun transform(hasListeners: Boolean, windowInsets: ExtendedWindowInsets): WindowInsetsCompat
}

interface InsetsProvider : InsetsListener {
    val current: WindowInsetsCompat

    fun View.onInit()
    fun addInsetsListener(listener: InsetsListener): Int
    fun removeInsetsListener(listener: InsetsListener)
    fun removeInsetsListener(key: Int)
    fun setInsetsModifier(modifier: InsetsModifier)
    // the one of the two entry points for system window insets
    // and hidden supertype override View.dispatchApplyWindowInsets(WindowInsets),
    // that allows not to set a insets listener that can be replaced with another one
    fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets
    fun requestInsets()
}

interface ViewInsetsConfig {
    fun padding(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false): ViewInsetsConfig
    fun margin(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false): ViewInsetsConfig
}

interface ViewInsetsDelegate {
    fun applyInsets(block: ViewInsetsConfig.() -> Unit): ViewInsetsDelegate
    fun applyInsets(start: InsetsDestination? = null, top: InsetsDestination? = null, end: InsetsDestination? = null, bottom: InsetsDestination? = null): ViewInsetsDelegate
    fun unsubscribeInsets(): ViewInsetsDelegate
    fun onApplyWindowInsets(windowInsets: WindowInsetsCompat)
}

enum class InsetsDestination(
    internal val label: String,
    internal val isNone: Boolean,
) {
    None("none", true), Padding("padding", false), Margin("margin", false);

    internal val letter: Char = label.first()
}

class ExtendedInsetsTypeMaskOverflow : Exception()

class MultipleViewInsetsDelegate(message: String?) : Exception(message ?: "")
