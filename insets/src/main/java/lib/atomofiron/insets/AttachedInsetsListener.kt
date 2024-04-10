/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.view.View

fun interface AttachedInsetsListener {
    fun detachInsetsListener()
}

/** attach the listener to the nearest insets provider */
fun View.attachInsetsListener(listener: InsetsListener): AttachedInsetsListener {
    val nameWithId = nameWithId()
    logd { "$nameWithId attach insets listener" }
    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            logd { "$nameWithId add attached insets listener?" }
            addInsetsListener(listener)
        }
        override fun onViewDetachedFromWindow(v: View) {
            logd { "$nameWithId remove attached insets listener?" }
            removeInsetsListener(listener)
        }
    }
    addOnAttachStateChangeListener(attachListener)
    if (isAttachedToWindow) addInsetsListener(listener)
    return AttachedInsetsListener {
        logd { "$nameWithId detach insets listener" }
        removeOnAttachStateChangeListener(attachListener)
        if (isAttachedToWindow) removeInsetsListener(listener)
    }
}

fun View.attachInsetsListener(
    triggers: TypeSet,
    listener: (windowInsets: ExtendedWindowInsets) -> Unit,
): AttachedInsetsListener = object : InsetsListener {
    override val triggers = triggers
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) = listener(windowInsets)
}.let { attachInsetsListener(it) }
