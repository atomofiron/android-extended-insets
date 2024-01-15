package lib.atomofiron.insets

import android.annotation.SuppressLint
import android.util.Log
import android.view.View

var debugInsets: Boolean = false

internal val Any.simpleName: String get() = javaClass.simpleName

@SuppressLint("ResourceType")
internal fun View.nameWithId(): String {
    val id = when {
        id <= 0 -> id.toString()
        else -> resources.getResourceEntryName(id)
    }
    return "$simpleName(id=$id)"
}

internal inline fun Any?.logd(message: () -> String) {
    if (debugInsets) Log.d("ExtInsets", "[${this?.simpleName}] ${message()}")
}

internal fun Int.bits(): String {
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
