package lib.atomofiron.demo

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

val View.visibleHeight: Int get() = if (isVisible) height else 0
