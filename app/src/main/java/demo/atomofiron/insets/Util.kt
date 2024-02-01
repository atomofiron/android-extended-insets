package demo.atomofiron.insets

import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginTop

fun <T> Array<T>.sumBy(zero: T? = null, action: T.(T) -> T): T = asIterable().sumBy(zero, action)

fun <T> Iterable<T>.sumBy(zero: T? = null, action: T.(T) -> T): T {
    val iter = iterator()
    if (!iter.hasNext()) return zero as T
    var sum = iter.next()
    while (iter.hasNext()) {
        sum = sum.action(iter.next())
    }
    return sum
}

val View.visibleTopHeight: Int get() = when {
    isVisible -> bottom - top + marginTop + translationY.toInt()
    else -> 0
}.coerceAtLeast(0)

val View.visibleBottomHeight: Int get() {
    val top = top + translationY.toInt()
    return when {
        !isVisible -> 0
        isInLayout && top == 0 -> 0
        else -> bottom - top + marginBottom
    }.coerceAtLeast(0)
}

val View.visibleLeftWidth: Int get() = when {
    isVisible -> right + translationX.toInt()
    else -> 0
}.coerceAtLeast(0)

val View.visibleRightWidth: Int get() {
    val left = left + translationX.toInt()
    return when {
        !isVisible -> 0
        isInLayout && left == 0 -> 0
        else -> (parent as View).width - left
    }.coerceAtLeast(0)
}
