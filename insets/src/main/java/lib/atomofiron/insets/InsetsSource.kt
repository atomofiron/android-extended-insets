/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import androidx.core.graphics.Insets
import java.util.Objects


open class InsetsSource private constructor(
    internal val types: TypeSet,
    internal val insets: Insets,
    next: InsetsSource?,
) : Set<InsetsSource> {
    companion object : InsetsSource(TypeSet.Empty, Insets.NONE, null) {
        private val emptyIterator = object : Iterator<InsetsSource> {
            override fun hasNext(): Boolean = false
            override fun next(): InsetsSource = throw NoSuchElementException()
        }
        override val size = 0
        override fun iterator() = emptyIterator
    }

    internal val next: InsetsSource? = next?.takeIf { it.isNotEmpty() }

    override val size: Int = (this.next?.size ?: 0).inc()

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

    override fun iterator() = object : Iterator<InsetsSource> {
        private var next: InsetsSource? = this@InsetsSource
            get() = field?.takeIf { it.isNotEmpty() }
        override fun hasNext(): Boolean = next != null
        override fun next(): InsetsSource = next
            ?.also { next = it.next }
            ?.single()
            ?: throw NoSuchElementException()
    }

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

    fun publish(types: TypeSet, insets: Insets) = InsetsSource(types, insets, next = takeIf { it.isNotEmpty() })

    private fun single(): InsetsSource = if (size == 1) this else InsetsSource(types, insets, next = null)

    private fun string() = "[$types][${insets.ltrb()}]"
}