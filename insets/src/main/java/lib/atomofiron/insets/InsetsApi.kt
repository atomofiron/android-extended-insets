/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets

fun interface InsetsListener {
    // provide the new insets only if at least one change's type matches with triggers (empty = any)
    val types: TypeSet get() = TypeSet.Empty
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
    fun submitInsets(key: Int, source: InsetsSource)
    fun revokeInsetsFrom(key: Int)
    fun dropNativeInsets(drop: Boolean = true)
    fun collectTypes(): TypeSet
}

interface ViewDelegate {
    val view: View?
    val nameWithId: String
}

interface ViewInsetsDelegate {
    fun resetInsets(block: ViewInsetsConfig.() -> Unit)
    fun combining(combining: InsetsCombining?)
    fun scrollOnEdge(): ViewInsetsDelegate
}

fun interface InsetsModifierCallback {
    fun modify(types: () -> TypeSet, windowInsets: ExtendedWindowInsets): ExtendedWindowInsets
}

interface ViewInsetsSource {
    fun invalidateInsets()
}

typealias ViewInsetsSourceCallback = (view: View) -> InsetsSource

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

class MultipleViewInsetsDelegate(message: String?) : Exception(message ?: "")

class NoMarginLayoutParams(message: String?) : Exception(message ?: "")
