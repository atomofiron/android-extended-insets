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
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


//                        /system-\
// 00000000000000000000000101010101
//            seeds: 31...987654321
internal const val LEGACY_LIMIT = Int.SIZE_BITS - 1
internal val LEGACY_RANGE = 1..LEGACY_LIMIT

class ExtendedWindowInsets private constructor(
    insets: Map<Int, InsetsValue>,
    windowInsets: WindowInsetsCompat?,
    // WindowInsetsCompat may be needed for getInsetsIgnoringVisibility()
) : WindowInsetsCompat(windowInsets) {
    companion object {
        val CONSUMED = ExtendedWindowInsets(WindowInsetsCompat.CONSUMED)
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

            internal val types = linkedSetOf(TypeSet.EMPTY, statusBars, navigationBars, captionBar, displayCutout, tappableElement, systemGestures, mandatorySystemGestures, ime)

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

        fun next(name: String) = TypeSet(name).also { types.add(it) }
    }

    internal val insets: Map<Int, InsetsValue> = insets.toMap()

    constructor(windowInsets: WindowInsets?, view: View? = null) : this(windowInsets?.let { toWindowInsetsCompat(it, view) })

    constructor(windowInsets: WindowInsetsCompat? = null) : this(windowInsets.toValues(), windowInsets)

    operator fun get(typeMask: Int): Insets = getInsets(typeMask)

    override fun getInsets(type: Int): Insets {
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

    operator fun get(types: TypeSet): Insets {
        val values = intArrayOf(0, 0, 0, 0)
        var next: TypeSet? = types
        while (next != null) {
            values.max(next.seed)
            next = next.next
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

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

    class Builder private constructor(
        values: Map<Int, InsetsValue>?,
        // WindowInsetsCompat may be needed for getInsetsIgnoringVisibility()
        private val windowInsets: WindowInsetsCompat?,
    ) {
        private val values: MutableMap<Int, InsetsValue> = values?.toMutableMap() ?: mutableMapOf()

        constructor(windowInsets: WindowInsets?, view: View? = null) : this(windowInsets?.let{ toWindowInsetsCompat(windowInsets, view) })

        constructor(windowInsets: WindowInsetsCompat? = null) : this(windowInsets.toValues(), windowInsets)

        operator fun set(type: Int, insets: Insets): Builder {
            for (seed in LEGACY_RANGE) {
                val cursor = seed.toTypeMask()
                when {
                    cursor > type -> break
                    (cursor and type) != 0 -> values[seed] = InsetsValue(insets)
                }
            }
            return this
        }

        operator fun set(types: TypeSet, insets: Insets): Builder {
            val insetsValue = InsetsValue(insets)
            logd { "set ${types.joinToString(separator = " ") { "${it.name}$insetsValue" }}" }
            types.forEach {
                values[it.seed] = insetsValue
            }
            return this
        }

        fun consume(typeMask: Int): Builder {
            consume(Insets.of(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), typeMask.toTypeSet())
            return this
        }

        fun consume(insets: Insets, types: TypeSet? = null): Builder {
            logConsuming(values, insets, types)
            when {
                insets.isEmpty() -> Unit
                types?.isEmpty() == true -> Unit
                types == null -> for ((seed, insetsValue) in values.entries.toList()) {
                    val value = insetsValue.consume(insets)
                    when {
                        value.isEmpty -> values.remove(seed)
                        else -> values[seed] = value
                    }
                }
                else -> for (type in types) {
                    values[type.seed]?.consume(insets)?.let {
                        when {
                            it.isEmpty -> values.remove(type.seed)
                            else -> values[type.seed] = it
                        }
                    }
                }
            }
            return this
        }

        // compatible with the WindowInsetsCompat api
        fun setInsets(type: Int, insets: Insets): Builder = set(type, insets)

        fun build(): ExtendedWindowInsets {
            logd { "build ${values.entries.joinToString(separator = " ") { "${it.key.getTypeName()}${it.value}" }}" }
            return ExtendedWindowInsets(values.toMap(), windowInsets)
        }
    }
}

// mask for each of the four parts of the ULong value
private const val PART_MASK = 0b1111111111111111uL

@JvmInline
internal value class InsetsValue(
    //                                                 /--PART_MASK---\
    // 1010101010101010101010101010101010101010101010101010101010101010
    // \-----left-----/\-----top------/\-----right----/\----bottom----/
    private val value: ULong = 0uL,
) {
    val isEmpty: Boolean get() = value == 0uL
    val left: Int get() = value.shr(48).toInt()
    val top: Int get() = (value.shr(32) and PART_MASK).toInt()
    val right: Int get() = (value.shr(16) and PART_MASK).toInt()
    val bottom: Int get() = (value and PART_MASK).toInt()

    constructor(left: Int, top: Int, right: Int, bottom: Int) : this(
        left.toULong().shl(48) +
                (top.toULong() and PART_MASK).shl(32) +
                (right.toULong() and PART_MASK).shl(16) +
                (bottom.toULong() and PART_MASK)
    )

    constructor(insets: Insets) : this(insets.left, insets.top, insets.right, insets.bottom)

    fun toInsets() = Insets.of(left, top, right, bottom)

    fun consume(insets: Insets): InsetsValue {
        return InsetsValue(
            (left - insets.left).coerceAtLeast(0),
            (top - insets.top).coerceAtLeast(0),
            (right - insets.right).coerceAtLeast(0),
            (bottom - insets.bottom).coerceAtLeast(0),
        )
    }

    override fun toString(): String = "[$left,$top,$right,$bottom]"
}

private fun WindowInsetsCompat?.toValues(): Map<Int, InsetsValue> {
    val insets = mutableMapOf<Int, InsetsValue>()
    this ?: return insets
    for (seed in LEGACY_RANGE) {
        val next = getInsets(seed.toTypeMask())
        if (next.isNotEmpty()) insets[seed] = InsetsValue(next)
    }
    return insets
}
