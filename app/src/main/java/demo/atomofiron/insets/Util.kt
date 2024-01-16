package demo.atomofiron.insets

import android.view.View
import androidx.core.view.isVisible

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

val View.visibleHeight: Int get() = when {
    isVisible -> bottom + translationY.toInt()
    else -> 0
}.coerceAtLeast(0)

val View.visibleHeightBottom: Int get() {
    val top = top + translationY.toInt()
    return when {
        !isVisible -> 0
        isInLayout && top == 0 -> 0
        else -> (parent as View).height - top
    }.coerceAtLeast(0)
}
