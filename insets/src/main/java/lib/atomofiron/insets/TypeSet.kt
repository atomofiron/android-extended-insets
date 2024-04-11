/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import java.util.Objects

// WindowInsetsCompat.Type.SIZE = 9
private var nextSeed = 1000 - 7

data class TypeSet internal constructor(
    val name: String,
    internal val seed: Int = nextSeed++,
    internal val next: TypeSet? = null,
    val animated: Boolean = next?.animated ?: false,
) : Set<TypeSet> {
    companion object {
        private const val ZERO_SEED = 0
        private const val EXTRA_SEED = -1
        internal const val FIRST_SEED = ZERO_SEED + 1
        internal val All = TypeSet("all", EXTRA_SEED)
        val Empty = TypeSet("empty", ZERO_SEED)
    }

    override val size: Int = (if (seed == ZERO_SEED) 0 else 1) + (next?.size ?: 0)

    override fun isEmpty(): Boolean = size == 0

    operator fun contains(seed: Int): Boolean = seed == this.seed || next != null && next.contains(seed)

    override operator fun contains(element: TypeSet): Boolean = contains(element.seed)

    override fun containsAll(elements: Collection<TypeSet>): Boolean {
        when {
            elements.isEmpty() -> return true
            isEmpty() -> return elements.find { !it.isEmpty() } == null
            elements is TypeSet -> return elements.find { !contains(it) } == null
            else -> for (element in elements) {
                if (!containsAll(element)) return false
            }
        }
        return true
    }

    override fun iterator() = object : Iterator<TypeSet> {
        private var next: TypeSet? = this@TypeSet
            get() = field?.takeIf { it.seed != ZERO_SEED }
        override fun hasNext(): Boolean = next != null
        override fun next(): TypeSet = next?.also { next = it.next } ?: throw NoSuchElementException()
    }

    // without sorting
    operator fun plus(other: TypeSet): TypeSet {
        var head = takeIf { seed != ZERO_SEED }
        var next: TypeSet? = other
        while (next != null) {
            when {
                next.seed == ZERO_SEED -> Unit
                head?.contains(next) == true -> Unit
                else -> head = next.copy(next = head)
            }
            next = next.next
        }
        return head ?: Empty
    }

    operator fun minus(other: TypeSet): TypeSet = when {
        isEmpty() -> this
        other.isEmpty() -> this
        else -> operation(other, contains = false)
    }

    operator fun times(other: TypeSet): TypeSet = when {
        isEmpty() -> this
        other.isEmpty() -> other
        else -> operation(other, contains = true)
    }

    // this and other should not be empty
    private fun operation(other: TypeSet, contains: Boolean): TypeSet {
        var head: TypeSet? = null
        for (it in this) {
            if ((it in other) == contains) {
                head = it.copy(next = head)
            }
        }
        return head ?: Empty
    }

    override fun toString(): String = next?.takeIf { it.isNotEmpty() }?.let { ("$it,$name") } ?: name

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is TypeSet -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int = Objects.hash(seed, next)
}
