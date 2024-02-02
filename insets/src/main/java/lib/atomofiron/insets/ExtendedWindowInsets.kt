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

import android.view.View
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


//                        /system-\
// 00000000000000000000000101010101
//            seeds: 30...987654321
internal const val LEGACY_LIMIT = Int.SIZE_BITS - 2
internal val LEGACY_RANGE = 1..LEGACY_LIMIT

class ExtendedWindowInsets internal constructor(
    insets: Map<Int, InsetsValue>,
    private val hidden: TypeSet = TypeSet.EMPTY,
    private val displayCutout: DisplayCutoutCompat?,
) {
    @Suppress("FunctionName")
    companion object {
        val EMPTY = ExtendedWindowInsets()

        fun Builder() = ExtendedBuilder()

        fun Builder(windowInsets: WindowInsets?, view: View? = null): ExtendedBuilder {
            return Builder(windowInsets?.let{ WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view) })
        }

        fun Builder(windowInsets: WindowInsetsCompat? = null): ExtendedBuilder {
            return ExtendedBuilder(windowInsets.getValues(), windowInsets.getHidden(), windowInsets?.displayCutout)
        }

        fun Builder(windowInsets: ExtendedWindowInsets? = null): ExtendedBuilder {
            return windowInsets?.run { ExtendedBuilder(insets, hidden, displayCutout) } ?: Builder()
        }
    }

    abstract class Type {
        companion object {
            val statusBars: TypeSet = WindowInsetsCompat.Type.statusBars().toTypeSet("statusBars")
            val navigationBars: TypeSet = WindowInsetsCompat.Type.navigationBars().toTypeSet("navigationBars")
            val captionBar: TypeSet = WindowInsetsCompat.Type.captionBar().toTypeSet("captionBar")
            val systemBars: TypeSet = statusBars + navigationBars + captionBar
            val displayCutout: TypeSet = WindowInsetsCompat.Type.displayCutout().toTypeSet("displayCutout")
            val barsWithCutout: TypeSet = systemBars + displayCutout
            val tappableElement: TypeSet = WindowInsetsCompat.Type.tappableElement().toTypeSet("tappableElement")
            val systemGestures: TypeSet = WindowInsetsCompat.Type.systemGestures().toTypeSet("systemGestures")
            val mandatorySystemGestures: TypeSet = WindowInsetsCompat.Type.mandatorySystemGestures().toTypeSet("mandatorySystemGestures")
            val ime: TypeSet = WindowInsetsCompat.Type.ime().toTypeSet("ime")
            val general: TypeSet = TypeSet("general")

            internal val types = linkedSetOf(TypeSet.EMPTY, statusBars, navigationBars, captionBar, displayCutout, tappableElement, systemGestures, mandatorySystemGestures, ime, general)

            inline operator fun invoke(block: Companion.() -> TypeSet): TypeSet = this.block()

            inline operator fun <T : Type> T.invoke(block: T.() -> TypeSet): TypeSet = block()
        }

        val statusBars = Companion.statusBars
        val navigationBars = Companion.navigationBars
        val captionBar = Companion.captionBar
        val systemBars = Companion.systemBars
        val displayCutout = Companion.displayCutout
        val barsWithCutout = Companion.barsWithCutout
        val tappableElement = Companion.tappableElement
        val systemGestures = Companion.systemGestures
        val mandatorySystemGestures = Companion.mandatorySystemGestures
        val ime = Companion.ime
        val general = Companion.general

        fun next(name: String) = TypeSet(name).also { types.add(it) }
    }

    internal val insets: Map<Int, InsetsValue> = insets.toMap()

    constructor(windowInsets: WindowInsets?, view: View? = null)
            : this(windowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it, view) })

    constructor(windowInsets: WindowInsetsCompat? = null)
            : this(windowInsets.getValues(), windowInsets.getHidden(), displayCutout = windowInsets?.displayCutout)

    @Deprecated("Compatibility with API of WindowInsets", replaceWith = ReplaceWith("get(type)"))
    fun getInsets(type: Int): Insets {
        var typeMask = 0
        (type.toTypeSet() - hidden).forEach {
            typeMask = typeMask or it.toLegacyType()
        }
        return when (typeMask) {
            0 -> Insets.NONE
            else -> getInsetsIgnoringVisibility(typeMask)
        }
    }

    @Deprecated("Compatibility with API of WindowInsets", replaceWith = ReplaceWith("getIgnoringVisibility(type)"))
    fun getInsetsIgnoringVisibility(type: Int): Insets {
        val values = intArrayOf(0, 0, 0, 0)
        var cursor = 1
        var seed = TypeSet.FIRST_SEED
        while (cursor in 1..type) {
            if ((cursor and type) != 0) {
                values.max(seed)
            }
            cursor = cursor.shl(1)
            seed++
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

    fun getIgnoringVisibility(types: TypeSet): Insets {
        if (types.isEmpty()) {
            return Insets.NONE
        }
        val values = intArrayOf(0, 0, 0, 0)
        var next: TypeSet? = types
        while (next != null) {
            values.max(next.seed)
            next = next.next
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

    operator fun get(types: TypeSet): Insets = getIgnoringVisibility(types - hidden)

    fun getDisplayCutout(): DisplayCutoutCompat? = displayCutout

    fun isVisible(type: TypeSet): Boolean = !hidden.contains(type)

    fun hasInsets(): Boolean = insets.count { !it.value.isEmpty } != 0

    fun isConsumed(): Boolean = false

    private fun IntArray.max(seed: Int) {
        val value = insets[seed]
        if (value?.isEmpty == false) {
            set(0, max(get(0), value.left))
            set(1, max(get(1), value.top))
            set(2, max(get(2), value.right))
            set(3, max(get(3), value.bottom))
        }
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val otherValues = (other as? ExtendedWindowInsets)?.insets ?: emptyMap()
        return otherValues == insets && super.equals(other)
    }

    override fun hashCode(): Int = 31 * insets.hashCode() + super.hashCode()
}

