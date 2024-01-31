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

        init {
            logd { "init ${this.values.entries.joinToString(separator = " ") { "${it.key.getTypeName()}${it.value}" }}" }
        }

        // compatible with the WindowInsetsCompat api
        fun setInsets(type: Int, insets: Insets): Builder {
            for (seed in LEGACY_RANGE) {
                val cursor = seed.toTypeMask()
                when {
                    cursor > type -> break
                    (cursor and type) != 0 -> values[seed] = insets.toValues()
                }
            }
            return this
        }

        fun consume(typeMask: Int): Builder {
            consume(Insets.of(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), typeMask.toTypeSet())
            return this
        }

        operator fun get(types: TypeSet): Insets {
            var value = InsetsValue()
            types.forEach {
                value = value.max(values[it.seed])
            }
            return value.toInsets()
        }

        operator fun set(types: TypeSet, insets: Insets): Builder {
            val debugValues = debug { values.toMap() }
            val insetsValue = insets.toValues()
            types.forEach {
                values[it.seed] = insetsValue
            }
            debugValues?.let { logd("set", from = it, to = values, insets, types) }
            return this
        }

        fun max(types: TypeSet, insets: Insets): Builder {
            val debugValues = debug { values.toMap() }
            val insetsValue = insets.toValues()
            types.forEach {
                values[it.seed] = insetsValue max values[it.seed]
            }
            debugValues?.let { logd("max", from = it, to = values, insets, types) }
            return this
        }

        fun add(types: TypeSet, insets: Insets): Builder {
            val debugValues = debug { values.toMap() }
            val insetsValue = insets.toValues()
            types.forEach {
                values[it.seed] = insetsValue + values[it.seed]
            }
            debugValues?.let { logd("add", from = it, to = values, insets, types) }
            return this
        }

        fun consume(insets: Insets, types: TypeSet? = null): Builder {
            val debugValues = debug { values.toMap() }
            when {
                insets.isEmpty() -> return this.also { logd { "consume empty" } }
                types?.isEmpty() == true -> return this.also { logd { "consume nothing" } }
                types == null -> for ((seed, value) in values.entries.toList()) {
                    values[seed] = value.consume(insets)
                }
                else -> for (type in types) {
                    values[type.seed] = values[type.seed]?.consume(insets) ?: continue
                }
            }
            debugValues?.let { logd("consume", from = it, to = values, insets, types) }
            return this
        }

        fun build(): ExtendedWindowInsets {
            logd { "build ${values.entries.joinToString(separator = " ") { "${it.key.getTypeName()}${it.value}" }}" }
            return ExtendedWindowInsets(values.toMap(), windowInsets)
        }
    }
}

