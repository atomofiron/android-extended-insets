package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat

fun interface InsetsListener {
    fun onApplyWindowInsets(windowInsets: WindowInsetsCompat)
}

fun interface InsetsModifier {
    fun getInsets(windowInsets: WindowInsetsCompat): WindowInsetsCompat
}

interface InsetsProvider {
    fun onInit(thisView: View)
    fun addListener(listener: InsetsListener)
    fun removeListener(listener: InsetsListener)
    fun setInsetsModifier(modifier: InsetsModifier)
    // the one of the two entry points for system window insets
    // and hidden supertype override View.dispatchApplyWindowInsets(WindowInsets),
    // that allows not to set a insets listener that can be replaced with another one
    fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets
}

enum class InsetsDestination {
    Padding, Margin
}

interface ViewInsetsDelegate {
    fun updatePaddingRelative(start: Int, top: Int, end: Int, bottom: Int)
    fun updateMarginRelative(start: Int, top: Int, end: Int, bottom: Int)
    fun apply(windowInsets: WindowInsetsCompat)
}
