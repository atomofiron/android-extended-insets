package lib.atomofiron.insets

import android.util.Log
import android.view.View
import androidx.core.view.isVisible


fun Any?.poop(s: String) {
    Log.e("atomofiron", "[${this?.javaClass?.simpleName}] $s")
}

val View.visibleHeight: Int get() = if (isVisible) height else 0

fun View.onAttachCallback(
    onAttach: (View) -> Unit,
    onDetach: (View) -> Unit,
) {
    if (isAttachedToWindow) onAttach(this)
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = onAttach(view)
        override fun onViewDetachedFromWindow(view: View) = onDetach(view)
    })
}