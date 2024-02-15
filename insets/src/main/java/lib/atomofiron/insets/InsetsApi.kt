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

fun interface InsetsListener {
    fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets)
}

interface InsetsProvider {
    val current: ExtendedWindowInsets

    fun View.onInit()
    fun addInsetsListener(listener: InsetsListener): Int
    fun removeInsetsListener(listener: InsetsListener)
    fun removeInsetsListener(key: Int)
    fun setInsetsModifier(modifier: InsetsModifierCallback)
    // the one of the two entry points for system window insets
    // and hidden supertype override View.dispatchApplyWindowInsets(WindowInsets),
    // that allows not to set a insets listener that can be replaced with another one
    fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets
    fun requestInsets()
    fun dropNativeInsets(drop: Boolean = true)
}

interface ViewInsetsDelegate : InsetsListener {
    fun resetInsets(block: ViewInsetsConfig.() -> Unit): ViewInsetsDelegate
    fun dependency(callback: InsetsCallback? = null): ViewInsetsDelegate
    fun dependency(
        horizontal: Boolean = false,
        vertical: Boolean = false,
        callback: InsetsCallback? = null,
    ): ViewInsetsDelegate
    fun combining(combining: InsetsCombining?)
    fun scrollOnEdge(): ViewInsetsDelegate
    fun detachFromProvider()
    fun detachFromView()
}

fun interface InsetsModifierCallback {
    fun transform(hasListeners: Boolean, windowInsets: ExtendedWindowInsets): ExtendedWindowInsets
}

fun interface InsetsDependencyCallback {
    fun getModifier(windowInsets: ExtendedWindowInsets): InsetsModifier?
}

fun interface InsetsCallback {
    operator fun invoke(arg: InsetsCallbackArg): InsetsModifier?
}

class InsetsCallbackArg(
    val view: View,
    val windowInsets: ExtendedWindowInsets,
) {
    operator fun component1() = view
    operator fun component2() = windowInsets
}

enum class InsetsDestination(
    internal val label: String,
    internal val isNone: Boolean,
) {
    None("none", true), Padding("padding", false), Margin("margin", false), Translation("translation", false);

    internal val letter: Char = label.first()

    override fun toString(): String = label
}

data class InsetsCombining(
    val combiningTypes: TypeSet,
    val minStart: Int = 0,
    val minTop: Int = 0,
    val minEnd: Int = 0,
    val minBottom: Int = 0,
)

class ExtendedInsetsTypeMaskOverflow : Exception()

class MultipleViewInsetsDelegate(message: String?) : Exception(message ?: "")

class NoMarginLayoutParams(message: String?) : Exception(message ?: "")
