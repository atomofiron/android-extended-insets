package lib.atomofiron.insets

import android.view.View

fun interface AttachedInsetsListener {
    fun detachInsetsListener()
}

fun View.attachInsetsListener(listener: InsetsListener): AttachedInsetsListener {
    logd { "${nameWithId()} attach insets listener" }
    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            logd { "${nameWithId()} add attached insets listener?" }
            addInsetsListener(listener)
        }
        override fun onViewDetachedFromWindow(v: View) {
            logd { "${nameWithId()} remove attached insets listener?" }
            removeInsetsListener(listener)
        }
    }
    addOnAttachStateChangeListener(attachListener)
    if (isAttachedToWindow) addInsetsListener(listener)
    return AttachedInsetsListener {
        logd { "${nameWithId()} detach insets listener" }
        removeOnAttachStateChangeListener(attachListener)
        if (isAttachedToWindow) removeInsetsListener(listener)
    }
}