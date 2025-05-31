package lib.atomofiron.insets

import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.WindowCompat

fun Window.reallyDisableFitsSystemWindows() {
    WindowCompat.setDecorFitsSystemWindows(this, false)
    // WindowCompat.setDecorFitsSystemWindows() is not enough
    decorView.giveMeFuckingInsets(4)
}

private fun View.giveMeFuckingInsets(downCount: Int) {
    fitsSystemWindows = false
    if (downCount > 0) (this as? ViewGroup)?.let {
        for (i in 0..<childCount) {
            getChildAt(i).giveMeFuckingInsets(downCount.dec())
        }
    }
}
