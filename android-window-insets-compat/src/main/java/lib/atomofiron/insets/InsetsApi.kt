package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat

fun interface InsetsListener {
    fun onApplyWindowInsets(windowInsets: WindowInsetsCompat)
}

fun interface InsetsModifier {
    fun getInsets(hasListeners: Boolean, windowInsets: WindowInsetsCompat): WindowInsetsCompat
}

interface InsetsProvider : InsetsListener {
    val current: WindowInsetsCompat

    fun onInit(thisView: View)
    fun addInsetsListener(listener: InsetsListener): Int
    fun removeInsetsListener(listener: InsetsListener)
    fun removeInsetsListener(key: Int)
    fun setInsetsModifier(modifier: InsetsModifier)
    // the one of the two entry points for system window insets
    // and hidden supertype override View.dispatchApplyWindowInsets(WindowInsets),
    // that allows not to set a insets listener that can be replaced with another one
    fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets
}

enum class InsetsDestination {
    None, Padding, Margin
}

interface ViewInsetsDelegate {
    fun padding(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false): ViewInsetsDelegate
    fun margin(start: Boolean = false, top: Boolean = false, end: Boolean = false, bottom: Boolean = false): ViewInsetsDelegate
    fun apply(windowInsets: WindowInsetsCompat)
    fun detach()
}
