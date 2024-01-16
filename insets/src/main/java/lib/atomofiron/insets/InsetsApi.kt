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
