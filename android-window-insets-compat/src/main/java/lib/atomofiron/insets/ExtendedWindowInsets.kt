package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max


// WindowInsetsCompat.Type.SIZE = 9
private const val OFFSET = 9
private const val LIMIT = Int.SIZE_BITS - OFFSET
// 9 bits from the right are for system window insets
// /-------custom--------\/system-\
// 10101010101010101010101010101010
// ^-last          FIRST-^
private const val FIRST = 1.shl(OFFSET)
private var next = FIRST

private fun emptyCustom(): Array<InsetsValue> = Array(LIMIT) { InsetsValue() }

class ExtendedWindowInsets private constructor(
    private val extended: Array<InsetsValue>,
    windowInsets: WindowInsetsCompat?,
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

        fun next(): Int = when (next) {
            0 -> throw ExtendedInsetsTypeMaskOverflow()
            else -> next.apply { next = shl(1) }
        }

        companion object : Type() {

            inline operator fun invoke(block: Companion.() -> Int): Int = this.block()

            inline operator fun <T : Type> T.invoke(block: T.() -> Int): Int = this.block()
        }
    }

    constructor() : this(null as WindowInsetsCompat?)

    constructor(windowInsets: WindowInsets?, view: View? = null) : this(windowInsets?.let { toWindowInsetsCompat(it, view) })

    constructor(windowInsets: WindowInsetsCompat?) : this(emptyCustom(), windowInsets)

    override fun getInsets(typeMask: Int): Insets = get(typeMask)

    operator fun get(type: Int): Insets = super.getInsets(type).union(type)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> return false
            !is ExtendedWindowInsets -> extended.isEmpty() && super.equals(other)
            else -> other.extended.contentEquals(extended)
        }
    }

    private fun Insets.union(type: Int): Insets {
        val values = intArrayOf(left, top, right, bottom)
        var cursor = FIRST
        var index = 0
        while (cursor <= type) {
            val value = extended[index++]
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

    override fun hashCode(): Int = 31 * extended.contentHashCode() + super.hashCode()

    class Builder private constructor(
        custom: Array<InsetsValue>?,
        windowInsets: WindowInsetsCompat?,
    ) {
        private val extended: Array<InsetsValue> = custom ?: emptyCustom()
        private val builder = WindowInsetsCompat.Builder(windowInsets ?: WindowInsetsCompat.CONSUMED)

        constructor() : this(custom = null, windowInsets = null)

        constructor(windowInsets: WindowInsets?, view: View? = null) : this(null, windowInsets?.let { toWindowInsetsCompat(it, view) })

        constructor(windowInsets: WindowInsetsCompat?) : this(null, windowInsets)

        constructor(extendedInsets: ExtendedWindowInsets?) : this(extendedInsets?.extended?.clone(), extendedInsets)

        // compatible with the WindowInsetsCompat api
        fun setInsets(type: Int, insets: Insets): Builder {
            builder.setInsets(type, insets)
            var cursor = FIRST
            var index = 0
            while (cursor <= type) {
                if (cursor and type != 0) {
                    extended[index] = InsetsValue(insets)
                }
                cursor = cursor.shl(1)
                index++
            }
            return this
        }

        operator fun set(type: Int, insets: Insets): Builder = setInsets(type, insets)

        fun build() = ExtendedWindowInsets(extended.clone(), builder.build())
    }
}

// mask for each of the four parts of the ULong value
private const val PART_MASK = 0b1111111111111111uL

@JvmInline
value class InsetsValue(
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

class ExtendedInsetsTypeMaskOverflow : Exception()
