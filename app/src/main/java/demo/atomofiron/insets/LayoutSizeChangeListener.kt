package demo.atomofiron.insets

import android.view.View

fun View.addLayoutSizeChangeListener(listener: (horizontal: Boolean, vertical: Boolean) -> Unit) {
    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        val horizontal = right - left != oldRight - oldLeft
        val vertical = bottom - top != oldBottom - oldTop
        if (horizontal || vertical) listener(horizontal, vertical)
    }
}

fun View.addLayoutWidthChangeListener(listener: (height: Int) -> Unit) {
    addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        val old = oldRight - oldLeft
        val new = right - left
        if (new != old) listener(new)
    }
}

fun View.addLayoutHeightChangeListener(listener: (height: Int) -> Unit) {
    addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
        val old = oldBottom - oldTop
        val new = bottom - top
        if (new != old) listener(new)
    }
}
