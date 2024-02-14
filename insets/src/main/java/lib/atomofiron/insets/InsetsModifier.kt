package lib.atomofiron.insets

import androidx.core.graphics.Insets
import java.util.Objects

internal enum class ModifierAction {
    None, Set, Max, Add, Consume
}

open class InsetsModifier private constructor(
    internal val action: ModifierAction,
    internal val types: TypeSet,
    internal val insets: Insets,
    next: InsetsModifier?,
) : Set<InsetsModifier> {
    companion object : InsetsModifier(ModifierAction.None, TypeSet.EMPTY, Insets.NONE, null) {
        private val emptyIterator = object : Iterator<InsetsModifier> {
            override fun hasNext(): Boolean = false
            override fun next(): InsetsModifier = throw NoSuchElementException()
        }
        override val size = 0
        override fun iterator() = emptyIterator
    }

    internal val next: InsetsModifier? = next?.takeIf { it.isNotEmpty() }

    override val size: Int = (next?.size ?: 0).inc()

    override fun isEmpty(): Boolean = size == 0

    override operator fun contains(element: InsetsModifier): Boolean {
        when {
            action != element.action -> Unit
            types != element.types -> Unit
            insets != element.insets -> Unit
            else -> return true
        }
        return next != null && next.contains(element)
    }

    override fun containsAll(elements: Collection<InsetsModifier>): Boolean {
        when {
            elements.isEmpty() -> return true
            isEmpty() -> return elements.find { !it.isEmpty() } == null
            elements is InsetsModifier -> return elements.find { !contains(it) } == null
            else -> for (element in elements) {
                if (!containsAll(element)) return false
            }
        }
        return true
    }

    override fun iterator() = object : Iterator<InsetsModifier> {
        private var next: InsetsModifier? = this@InsetsModifier
            get() = field?.takeIf { it.isNotEmpty() }
        override fun hasNext(): Boolean = next != null
        override fun next(): InsetsModifier = next?.also { next = it.next } ?: throw NoSuchElementException()
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is InsetsModifier -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int = Objects.hash(action, types, insets, next)

    override fun toString(): String = next?.let { ("${string()}, $it") } ?: string()

    private fun string() = "[$types]${action.name.lowercase()}[${insets.ltrb()}]"

    fun set(types: TypeSet, insets: Insets) = InsetsModifier(ModifierAction.Set, types, insets, this)

    fun max(types: TypeSet, insets: Insets) = InsetsModifier(ModifierAction.Max, types, insets, this)

    fun add(types: TypeSet, insets: Insets) = InsetsModifier(ModifierAction.Add, types, insets, this)

    fun consume(types: TypeSet, insets: Insets) = InsetsModifier(ModifierAction.Consume, types, insets, this)

    fun consume(types: TypeSet) = InsetsModifier(ModifierAction.Consume, types, MAX_INSETS, this)

    fun consume(insets: Insets) = InsetsModifier(ModifierAction.Consume, TypeSet.ALL, insets, this)
}