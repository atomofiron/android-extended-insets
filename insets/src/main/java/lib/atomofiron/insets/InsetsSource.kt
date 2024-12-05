/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import androidx.core.graphics.Insets
import java.util.Objects

class InsetsSource private constructor(
    val types: TypeSet,
    val insets: Insets,
    val next: InsetsSource?,
    val debugData: String? = null,
) : Set<InsetsSource> {
    companion object {
        val Empty = InsetsSource(TypeSet.Empty, Insets.NONE, next = null)
        private val emptyIterator = object : Iterator<InsetsSource> {
            override fun hasNext(): Boolean = false
            override fun next(): InsetsSource = throw NoSuchElementException()
        }

        fun submit(types: TypeSet, insets: Insets) = Empty.submit(types, insets)
    }

    override val size: Int = when {
        this === Empty -> 0
        next == null -> 1
        else -> next.size.inc()
    }

    override fun isEmpty(): Boolean = size == 0

    override operator fun contains(element: InsetsSource): Boolean {
        when {
            types != element.types -> Unit
            insets != element.insets -> Unit
            else -> return true
        }
        return next != null && next.contains(element)
    }

    override fun containsAll(elements: Collection<InsetsSource>): Boolean {
        when {
            elements.isEmpty() -> return true
            isEmpty() -> return !elements.any { !it.isEmpty() }
            elements is InsetsSource -> return !elements.any { !contains(it) }
            else -> for (element in elements) {
                if (!containsAll(element)) return false
            }
        }
        return true
    }

    override fun iterator() = if (isEmpty()) emptyIterator else IteratorImpl(this)

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is InsetsSource -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int = Objects.hash(types, insets, next)

    override fun toString(): String = next
        ?.takeIf { it.isNotEmpty() }
        ?.let { if (isEmpty()) it.toString() else "$it,${string()}" }
        ?: string()

    fun submit(types: TypeSet, insets: Insets): InsetsSource = when {
        types.isEmpty() -> this
        else -> InsetsSource(types, insets, next = takeIf { it.isNotEmpty() })
    }

    internal fun copy(debugData: String) = InsetsSource(types, insets, next, debugData)

    private fun single(): InsetsSource = if (next == null) this else InsetsSource(types, insets, next = null)

    private fun string() = "[$types][${insets.ltrb()}]"

    private inner class IteratorImpl(first: InsetsSource) : Iterator<InsetsSource> {
        private var next: InsetsSource? = first
            get() = field?.takeIf { it.isNotEmpty() }

        override fun hasNext(): Boolean = next != null

        override fun next(): InsetsSource = next
            ?.also { next = it.next }
            ?.single()
            ?: throw NoSuchElementException()
    }
}