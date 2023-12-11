package lib.atomofiron.demo

import android.view.View

fun View.addLayoutSizeChangeListener(listener: (Boolean, Boolean) -> Unit) {
    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        val horizontal = right - left != oldRight - oldLeft
        val vertical = bottom - top != oldBottom - oldTop
        listener(horizontal, vertical)
    }
}

fun View.addLayoutHeightChangeListener(listener: (Int) -> Unit) {
    addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
        val old = oldBottom - oldTop
        val new = bottom - top
        if (new != old) listener(new)
    }
}

fun View.requestApplyInsetsOnNextLayoutHeightChange() {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            removeOnLayoutChangeListener(this)
            val old = oldBottom - oldTop
            val new = bottom - top
            if (new != old) requestApplyInsets()
        }
    })
}