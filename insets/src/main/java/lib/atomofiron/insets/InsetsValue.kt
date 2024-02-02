package lib.atomofiron.insets

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import kotlin.math.max


// mask for each of the four parts of the ULong value
private const val PART_MASK = 0b1111111111111111uL
internal const val MAX_INSET = 0xffff

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

    override fun toString(): String = "[$left,$top,$right,$bottom]"
}

internal fun InsetsValue.toInsets() = Insets.of(left, top, right, bottom)

internal fun Insets.toValues() = InsetsValue(left, top, right, bottom)

internal operator fun InsetsValue.plus(other: InsetsValue?): InsetsValue {
    other ?: return this
    return InsetsValue(
        left + other.left,
        top + other.top,
        right + other.right,
        bottom + other.bottom,
    )
}

internal infix fun InsetsValue.max(other: InsetsValue?): InsetsValue {
    other ?: return this
    return InsetsValue(
        max(left, other.left),
        max(top, other.top),
        max(right, other.right),
        max(bottom, other.bottom),
    )
}

internal infix fun InsetsValue.consume(insets: Insets): InsetsValue {
    return InsetsValue(
        (left - insets.left).coerceAtLeast(0),
        (top - insets.top).coerceAtLeast(0),
        (right - insets.right).coerceAtLeast(0),
        (bottom - insets.bottom).coerceAtLeast(0),
    )
}
