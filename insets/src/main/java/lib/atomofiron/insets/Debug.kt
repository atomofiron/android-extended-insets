package lib.atomofiron.insets

import android.annotation.SuppressLint
import android.util.Log
import android.view.View

var debugInsets: Boolean = false

val Any.simpleName: String get() = javaClass.simpleName

@SuppressLint("ResourceType")
fun View.nameAndId(): String {
    val id = when {
        id <= 0 -> id.toString()
        else -> resources.getResourceEntryName(id)
    }
    return "$simpleName(id=$id)"
}

fun Any?.logd(s: String) {
    if (debugInsets) Log.d("ExtInsets", "[${this?.simpleName}] $s")
}

fun Int.bits(): String {
    val value = toUInt()
    val builder = StringBuilder(32)
    var cursor = 1u.shl(31)
    var flag = false
    for (i in 0..<32) {
        val bit = (value and cursor) != 0u
        flag = flag || bit
        if (flag) {
            builder.append(if (bit) "1" else "0")
        }
        cursor = cursor.shr(1)
    }
    if (builder.isEmpty()) {
        builder.append("0")
    }
    return builder.toString()
}
