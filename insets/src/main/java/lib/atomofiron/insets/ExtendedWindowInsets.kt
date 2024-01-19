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


// WindowInsetsCompat.Type.SIZE = 9
private const val OFFSET = 9
private const val LIMIT = Int.SIZE_BITS - 1 // one for the sign
//  /-------custom-------\/system-\
// 01010101010101010101010101010101
//  ^-last         FIRST-^
internal const val FIRST = 1.shl(OFFSET)
private var next = FIRST

private fun emptyValues(): Array<InsetsValue> = Array(LIMIT) { InsetsValue() }

private val emptyValues: Array<InsetsValue> = emptyValues()

class ExtendedWindowInsets private constructor(
    private val insets: Array<InsetsValue>,
    windowInsets: WindowInsetsCompat?,
    // WindowInsetsCompat may be needed for getInsetsIgnoringVisibility()
) : WindowInsetsCompat(windowInsets) {
    companion object {
        val CONSUMED = ExtendedWindowInsets(WindowInsetsCompat.CONSUMED)
    }

    abstract class Type {

        val statusBars: Int get() = WindowInsetsCompat.Type.statusBars()
        val navigationBars: Int get() = WindowInsetsCompat.Type.navigationBars()
        val systemBars: Int get() = WindowInsetsCompat.Type.systemBars()
        val displayCutout: Int get() = WindowInsetsCompat.Type.displayCutout()
        val tappableElement: Int get() = WindowInsetsCompat.Type.tappableElement()
        val systemGestures: Int get() = WindowInsetsCompat.Type.systemGestures()
        val mandatorySystemGestures: Int get() = WindowInsetsCompat.Type.mandatorySystemGestures()
        val ime: Int get() = WindowInsetsCompat.Type.ime()
        val captionBar: Int get() = WindowInsetsCompat.Type.captionBar()

        fun next(): Int = when {
            next <= 0 -> throw ExtendedInsetsTypeMaskOverflow()
            else -> next.apply { next = shl(1) }
        }

        companion object : Type() {

            inline operator fun invoke(block: Companion.() -> Int): Int = this.block()

            inline operator fun <T : Type> T.invoke(block: T.() -> Int): Int = this.block()
        }
    }

    constructor() : this(null as WindowInsetsCompat?)

    constructor(windowInsets: WindowInsets?, view: View? = null) : this(windowInsets?.let { toWindowInsetsCompat(it, view) })

    constructor(windowInsets: WindowInsetsCompat?) : this(windowInsets.toValues(), windowInsets)

    override fun getInsets(typeMask: Int): Insets = get(typeMask)

    operator fun get(type: Int): Insets {
        val values = intArrayOf(0, 0, 0, 0)
        var cursor = 1
        var index = 0
        while (cursor in 1..type) {
            val value = insets[index++]
            if (!value.isZero && (cursor and type) != 0) {
                values[0] = max(values[0], value.left)
                values[1] = max(values[1], value.top)
                values[2] = max(values[2], value.right)
                values[3] = max(values[3], value.bottom)
            }
            cursor = cursor.shl(1)
        }
        return Insets.of(values[0], values[1], values[2], values[3])
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        val otherValues = (other as? ExtendedWindowInsets)?.insets ?: emptyValues
        return otherValues.contentEquals(insets) && super.equals(other)
    }

    override fun hashCode(): Int = 31 * insets.contentHashCode() + super.hashCode()

    class Builder private constructor(
        values: Array<InsetsValue>?,
        // WindowInsetsCompat may be needed for getInsetsIgnoringVisibility()
        private val windowInsets: WindowInsetsCompat?,
    ) {
        private val values: Array<InsetsValue> = values ?: emptyValues()

        constructor(
            windowInsets: WindowInsets?,
            view: View? = null,
        ) : this(windowInsets?.let{ toWindowInsetsCompat(windowInsets, view) })

        constructor(
            windowInsets: WindowInsetsCompat? = null,
        ) : this(windowInsets.toValues(), windowInsets)

        constructor(
            extendedInsets: ExtendedWindowInsets?,
            windowInsets: WindowInsetsCompat?,
        ) : this(extendedInsets?.insets?.clone(), windowInsets)

        operator fun set(type: Int, insets: Insets): Builder {
            for (index in values.indices) {
                val cursor = 1.shl(index)
                when {
                    cursor > type -> break
                    (cursor and type) != 0 -> values[index] = InsetsValue(insets)
                }
            }
            return this
        }

        // compatible with the WindowInsetsCompat api
        fun setInsets(type: Int, insets: Insets): Builder = set(type, insets)

        fun build() = ExtendedWindowInsets(values.clone(), windowInsets)
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
    val isZero: Boolean get() = value == 0uL
    val left: Int get() = value.shr(48).toInt()
    val top: Int get() = (value.shr(32) and PART_MASK).toInt()
    val right: Int get() = (value.shr(16) and PART_MASK).toInt()
    val bottom: Int get() = (value and PART_MASK).toInt()

    constructor(insets: Insets) : this(
        insets.left.toULong().shl(48) +
                (insets.top.toULong() and PART_MASK).shl(32) +
                (insets.right.toULong() and PART_MASK).shl(16) +
                (insets.bottom.toULong() and PART_MASK)
    )

    fun toInsets() = Insets.of(left, top, right, bottom)
}

private fun WindowInsetsCompat?.toValues(): Array<InsetsValue> {
    val insets = emptyValues()
    this ?: return insets
    for (index in insets.indices) {
        val next = getInsets(1.shl(index))
        if (next.isNotEmpty()) insets[index] = InsetsValue(next)
    }
    return insets
}
