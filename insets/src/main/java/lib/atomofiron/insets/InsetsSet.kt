package lib.atomofiron.insets

import androidx.core.graphics.Insets
import java.util.Objects

internal enum class DepAction {
    None, Set, Max, Add, Consume
}

open class InsetsSet private constructor(
    internal val action: DepAction,
    internal val types: TypeSet,
    internal val insets: Insets,
    internal val next: InsetsSet?,
) : Set<InsetsSet> {
    companion object : InsetsSet(DepAction.None, TypeSet.EMPTY, Insets.NONE, null) {
        private val emptyIterator = object : Iterator<InsetsSet> {
            override fun hasNext(): Boolean = false
            override fun next(): InsetsSet = throw NoSuchElementException()
        }
        override val isEmpty = true
        override val size: Int = 0
        override fun iterator() = emptyIterator
    }

    internal open val isEmpty = false

    override val size: Int = (next?.size ?: 0).inc()

    override fun isEmpty(): Boolean = size == 0

    override operator fun contains(element: InsetsSet): Boolean {
        when {
            action != element.action -> Unit
            types != element.types -> Unit
            insets != element.insets -> Unit
            else -> return true
        }
        return next != null && next.contains(element)
    }

    override fun containsAll(elements: Collection<InsetsSet>): Boolean {
        when {
            elements.isEmpty() -> return true
            isEmpty() -> return elements.find { !it.isEmpty() } == null
            elements is InsetsSet -> return elements.find { !contains(it) } == null
            else -> for (element in elements) {
                if (!containsAll(element)) return false
            }
        }
        return true
    }

    override fun iterator() = object : Iterator<InsetsSet> {
        private var next: InsetsSet? = this@InsetsSet.takeIf { !isEmpty }
        override fun hasNext(): Boolean = next != null
        override fun next(): InsetsSet = next?.also { current ->
            next = current.next?.takeIf { !it.isEmpty }
        } ?: throw NoSuchElementException()
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is InsetsSet -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int = Objects.hash(action, types, insets, next)

    fun set(types: TypeSet, insets: Insets) = InsetsSet(DepAction.Set, types, insets, this)

    fun max(types: TypeSet, insets: Insets) = InsetsSet(DepAction.Max, types, insets, this)

    fun add(types: TypeSet, insets: Insets) = InsetsSet(DepAction.Add, types, insets, this)

    fun consume(types: TypeSet, insets: Insets) = InsetsSet(DepAction.Consume, types, insets, this)

    fun consume(types: TypeSet) = InsetsSet(DepAction.Consume, types, MAX_INSETS, this)
}