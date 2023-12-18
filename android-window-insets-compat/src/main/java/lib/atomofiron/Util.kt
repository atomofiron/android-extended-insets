package lib.atomofiron

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.core.view.isVisible

@SuppressLint("ResourceType")
fun View.id(): String {
    return when {
        id <= 0 -> "${javaClass.simpleName}($id)"
        else -> resources.getResourceEntryName(id)
    }
}

fun Any?.poop(s: String) {
    Log.e("atomofiron", "[${this?.javaClass?.simpleName}] $s")
}

fun Int.bits(
    prefix: String = "0x",
    zero: Char = '0',
    one: Char = '1',
    full: Boolean = false,
    oneDigitAtLeast: Boolean = true,
): String = toULong().bits(prefix, zero, one, 32, full, oneDigitAtLeast)

fun ULong.bits(
    prefix: String = "0x",
    zero: Char = '0',
    one: Char = '1',
    full: Boolean = false,
    oneDigitAtLeast: Boolean = true,
): String = bits(prefix, zero, one, 64, full, oneDigitAtLeast)

fun ULong.bits(
    prefix: String,
    zero: Char,
    one: Char,
    size: Int,
    full: Boolean,
    oneDigitAtLeast: Boolean,
): String {
    val builder = StringBuilder(size + prefix.length).append(prefix)
    var cursor = 1uL.shl(size.dec())
    var flag = full
    for (i in 0..<size) {
        val bit = (this and cursor) != 0uL
        flag = flag || bit
        if (flag) {
            builder.append(if (bit) one else zero)
        }
        cursor = cursor.shr(1)
    }
    if (oneDigitAtLeast && builder.length == prefix.length) {
        builder.append(zero)
    }
    return builder.toString()
}

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

val View.visibleHeight: Int get() = if (isVisible) height else 0
