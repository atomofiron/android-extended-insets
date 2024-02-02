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

import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

// WindowInsetsCompat.Type.SIZE = 9
private var nextSeed = AtomicInteger(1000-7)

data class TypeSet internal constructor(
    internal val name: String,
    internal val seed: Int = nextSeed.getAndAdd(1),
    internal val next: TypeSet? = null,
) : Set<TypeSet> {
    companion object {
        private const val ZERO_SEED = 0
        internal const val FIRST_SEED = ZERO_SEED + 1
        val EMPTY = TypeSet("empty", ZERO_SEED)
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
        private var next: TypeSet? = this@TypeSet.takeIf { seed != ZERO_SEED }
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
        return head ?: EMPTY
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
        forEach {
            if ((it in other) == contains) {
                head = it.copy(next = head)
            }
        }
        return head ?: EMPTY
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is TypeSet -> false
        !containsAll(other) -> false
        !other.containsAll(this) -> false
        else -> true
    }

    override fun hashCode(): Int {
        return when (next) {
            null -> Objects.hashCode(seed)
            else -> Objects.hash(seed, next.hashCode())
        }
    }
}
