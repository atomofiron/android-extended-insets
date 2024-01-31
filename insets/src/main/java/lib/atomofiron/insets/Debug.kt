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
import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import kotlin.reflect.KClass

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

internal inline fun <T : Any?> debug(action: () -> T): T? = if (debugInsets) action() else null

internal inline fun <T : Any?> T.logd(parent: KClass<*>? = null, message: T.() -> String) {
    if (debugInsets) {
        val parentClass = parent?.simpleName?.let { "$it." } ?: ""
        Log.d("ExtInsets", "[$parentClass${this?.simpleName}] ${message()}")
    }
}

internal fun Int.getTypeName(): String = Type.types.find { it.seed == this }?.name?.takeIf { it.isNotEmpty() } ?: "unknown"

internal fun TypeSet.getTypes(windowInsets: ExtendedWindowInsets?, left: Boolean, top: Boolean, right: Boolean, bottom: Boolean): String {
    val insetsMap = windowInsets?.insets ?: emptyMap()
    val dependencies = mutableListOf<String>()
    val not = mutableListOf<String>()
    insetsMap.entries.forEach { (seed, insets) ->
        val name = seed.getTypeName()
        when (true) {
            !contains(seed) -> Unit
            (left && insets.left > 0),
            (top && insets.top > 0),
            (right && insets.right > 0),
            (bottom && insets.bottom > 0) -> dependencies.add(name)
            else -> not.add(name)
        }
    }
    dependencies.replaceBars()
    not.replaceBars()
    return "${dependencies.joinToString(prefix = "dependsOn[", separator = ",", postfix = "]")} ${not.joinToString(prefix = "not[", separator = ",", postfix = "]")}"
}

private fun MutableList<String>.replaceBars() {
    val replacement = listOf(Type.statusBars.name, Type.navigationBars.name, Type.captionBar.name)
    if (containsAll(replacement)) {
        removeAll(replacement)
        add("systemBars")
    }
}

internal fun ExtendedWindowInsets.Builder.logd(
    operation: String,
    from: Map<Int, InsetsValue>,
    to: Map<Int, InsetsValue>,
    insets: Insets,
    types: TypeSet?,
) {
    logd(ExtendedWindowInsets::class) {
        val changes = from.mapNotNull { (seed, value) ->
            (to[seed] ?: InsetsValue()).takeIf { it != value }?.let { new ->
                val dl = (new.left - value.left).delta()
                val dt = (new.top - value.top).delta()
                val dr = (new.right - value.right).delta()
                val db = (new.bottom - value.bottom).delta()
                "${seed.getTypeName()}[${value.left}$dl,${value.top}$dt,${value.right}$dr,${value.bottom}$db]"
            }
        } + to.filter { from[it.key] == null }.map { (seed, value) ->
            "${seed.getTypeName()}[${value.left.delta0()},${value.top.delta0()},${value.right.delta0()},${value.bottom.delta0()}]"
        }
        val values = insets.run { "[$left,$top,$right,$bottom]" }
        val typeNames = types?.joinToString(separator = ",") { it.name }?.let { " types: $it," } ?: ""
        "$operation: $values,$typeNames changes: ${changes.joinToString(separator = " ")}"
    }
}

private fun Int.delta(): String = when {
    this < 0 -> "$this"
    this > 0 -> "+$this"
    else -> ""
}

private fun Int.delta0(): String = when {
    this < 0 -> "$this"
    this > 0 -> "+$this"
    else -> "0"
}
