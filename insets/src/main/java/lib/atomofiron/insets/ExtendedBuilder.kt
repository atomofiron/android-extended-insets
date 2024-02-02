package lib.atomofiron.insets

import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat

class ExtendedBuilder internal constructor(
    values: Map<Int, InsetsValue>? = null,
    private var hidden: TypeSet = TypeSet.EMPTY,
    private val displayCutout: DisplayCutoutCompat? = null,
) {
    private val values: MutableMap<Int, InsetsValue> = values?.toMutableMap() ?: mutableMapOf()

    init {
        logd { "init ${this.values.entries.joinToString(separator = " ") { "${it.key.getTypeName()}${it.value}" }}" }
    }

    @Deprecated("Compatibility with API of WindowInsets.Builder", replaceWith = ReplaceWith("set(type, insets)"))
    fun setInsets(type: Int, insets: Insets): ExtendedBuilder {
        for (seed in LEGACY_RANGE) {
            val cursor = seed.toTypeMask()
            when {
                cursor > type -> break
                (cursor and type) != 0 -> values[seed] = insets.toValues()
            }
        }
        return this
    }

    fun consume(typeMask: Int): ExtendedBuilder {
        consume(Insets.of(MAX_INSET, MAX_INSET, MAX_INSET, MAX_INSET), typeMask.toTypeSet())
        return this
    }

    operator fun get(types: TypeSet): Insets {
        var value = InsetsValue()
        types.forEach {
            value = value.max(values[it.seed])
        }
        return value.toInsets()
    }

    operator fun set(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue
        }
        debugValues?.let { logd("set", from = it, to = values, insets, types) }
        return this
    }

    fun max(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue max values[it.seed]
        }
        debugValues?.let { logd("max", from = it, to = values, insets, types) }
        return this
    }

    fun add(types: TypeSet, insets: Insets): ExtendedBuilder {
        val debugValues = debug { values.toMap() }
        val insetsValue = insets.toValues()
        types.forEach {
            values[it.seed] = insetsValue + values[it.seed]
        }
        debugValues?.let { logd("add", from = it, to = values, insets, types) }
        return this
    }

    fun consume(types: TypeSet): ExtendedBuilder {
        consume(Insets.of(MAX_INSET, MAX_INSET, MAX_INSET, MAX_INSET), types)
        return this
    }

    fun consume(insets: Insets, types: TypeSet? = null): ExtendedBuilder {
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

    fun setVisible(types: TypeSet): ExtendedBuilder {
        hidden -= types
        return this
    }

    fun setInvisible(types: TypeSet): ExtendedBuilder {
        hidden += types
        return this
    }

    fun setVisibility(types: TypeSet, visible: Boolean): ExtendedBuilder {
        if (visible) setVisible(types) else setInvisible(types)
        return this
    }

    fun build(): ExtendedWindowInsets {
        logd { "build ${values.entries.joinToString(separator = " ") { "${it.key.getTypeName()}${it.value}" }}" }
        return ExtendedWindowInsets(values.toMap(), hidden, displayCutout)
    }
}