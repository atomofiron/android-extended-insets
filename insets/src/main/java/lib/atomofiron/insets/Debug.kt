/*
 * Copyright 2024 Yaroslav Nesterov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.atomofiron.insets

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type

var debugInsets: Boolean = false

var customTypeNameProvider: ((Int) -> String?)? = null

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

private class Data(
    val windowInsets: WindowInsetsCompat?,
    val left: Boolean,
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
) {
    val dependencies = mutableListOf<String>()
    val not = mutableListOf<String>()
}

internal fun Int.getTypes(windowInsets: WindowInsetsCompat?, left: Boolean, top: Boolean, right: Boolean, bottom: Boolean): String {
    val data = Data(windowInsets, left, top, right, bottom)
    if (!check("systemBars", Type.systemBars(), data)) {
        check("statusBars", Type.statusBars(), data)
        check("navigationBars", Type.navigationBars(), data)
        check("captionBar", Type.captionBar(), data)
    }
    check("tappableElement", Type.tappableElement(), data)
    check("displayCutout", Type.displayCutout(), data)
    check("ime", Type.ime(), data)
    check("systemGestures", Type.systemGestures(), data)
    check("mandatorySystemGestures", Type.mandatorySystemGestures(), data)
    val customTypeNameProvider = customTypeNameProvider
    var cursor = FIRST
    while (this >= cursor && cursor > 0) {
        if ((this and cursor) != 0) {
            val name = customTypeNameProvider?.invoke(cursor) ?: "unknown"
            check(name, cursor, data)
        }
        cursor = cursor.shl(1)
    }
    return "${data.dependencies.joinToString(prefix = "dependsOn[", separator = ",", postfix = "]")} ${data.not.joinToString(prefix = "not[", separator = ",", postfix = "]")}"
}

private fun Int.check(name: String, type: Int, data: Data): Boolean {
    val matches = (this and type) == type
    when {
        !matches -> Unit
        data.depends(type) -> data.dependencies.add(name)
        else -> data.not.add(name)
    }
    return matches
}

private fun Data.depends(typeMask: Int): Boolean {
    val insets = windowInsets?.getInsets(typeMask)
    return when {
        insets == null -> false
        left && insets.left > 0 -> true
        top && insets.top > 0 -> true
        right && insets.right > 0 -> true
        bottom && insets.bottom > 0 -> true
        else -> false
    }
}
