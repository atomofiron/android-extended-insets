/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import kotlin.reflect.KClass

internal const val NAME_UNDEFINED = "undefined"

private var debug = false

fun setInsetsDebug(enabled: Boolean) {
    debug = enabled
}

internal val Any.simpleName: String get() = javaClass.simpleName

@SuppressLint("ResourceType")
internal fun View.nameWithId(): String {
    val id = when {
        id <= 0 -> id.toString()
        else -> resources.getResourceEntryName(id)
    }
    return "$simpleName(id=$id)"
}

internal inline fun <T : Any?> debug(action: () -> T): T? = if (debug) action() else null

internal inline fun <T : Any?> T.logd(parent: KClass<*>? = null, message: T.() -> String) {
    if (debug) {
        val parentClass = parent?.simpleName?.let { "$it." } ?: ""
        Log.d("ExtInsets", "[$parentClass${this?.simpleName}] ${message()}")
    }
}

internal fun Int.getTypeName(): String = Type.types.find { it.seed == this }?.name?.takeIf { it.isNotEmpty() } ?: NAME_UNDEFINED

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
    val dependsOn = dependencies.joinToString(prefix = "dependsOn[", separator = ",", postfix = "]")
    val notDepends = not.joinToString(prefix = "not[", separator = ",", postfix = "]")
    return "$dependsOn $notDepends"
}

private fun MutableList<String>.replaceBars() {
    val replaceable = listOf(Type.statusBars.name, Type.navigationBars.name, Type.captionBar.name)
    if (containsAll(replaceable)) {
        removeAll(replaceable)
        add("systemBars")
    }
}

internal fun ExtendedBuilder.logChanges(
    operation: String,
    from: Map<Int, InsetsValue>,
    to: Map<Int, InsetsValue>,
    insets: Insets,
    types: TypeSet?,
) {
    logd(ExtendedWindowInsets::class) {
        val changesList = from.mapNotNull { (seed, value) ->
            (to[seed] ?: InsetsValue()).takeIf { it != value }?.let { new ->
                val dl = (new.left - value.left).deltaOrEmpty()
                val dt = (new.top - value.top).deltaOrEmpty()
                val dr = (new.right - value.right).deltaOrEmpty()
                val db = (new.bottom - value.bottom).deltaOrEmpty()
                "${seed.getTypeName()}[${value.left}$dl,${value.top}$dt,${value.right}$dr,${value.bottom}$db]"
            }
        } + to.filter { from[it.key] == null }.map { (seed, value) ->
            val left = value.left.deltaOrZero()
            val top = value.top.deltaOrZero()
            val right = value.right.deltaOrZero()
            val bottom = value.bottom.deltaOrZero()
            "${seed.getTypeName()}[$left,$top,$right,$bottom]"
        }
        val changes = changesList.joinToString(separator = " ").ifEmpty { "none" }
        val values = insets.run { "[${left.orMax()},${top.orMax()},${right.orMax()},${bottom.orMax()}]" }
        val typeNames = types?.joinToString(separator = ",") { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?.let { " types: $it," }
            ?: ""
        "$operation: $values,$typeNames changes: $changes"
    }
}

private fun Int.orMax(): String = if (this == MAX_INSET) "max" else toString()

private fun Int.deltaOrEmpty(): String = deltaOr("")

private fun Int.deltaOrZero(): String = deltaOr("0")

private fun Int.deltaOr(zero: String): String = when {
    this < 0 -> "$this"
    this > 0 -> "+$this"
    else -> zero
}
