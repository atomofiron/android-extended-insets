package lib.atomofiron.insets

import androidx.core.graphics.Insets
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UnitTests {

    object CustomType : ExtendedWindowInsets.Type() {
        val testType1 = next()
        val testType2 = next()
        val testType3 = next()
    }

    // associate your custom type with ExtendedWindowInsets
    operator fun ExtendedWindowInsets.invoke(block: CustomType.() -> Int): Insets = get(CustomType.block())

    @Test
    fun ext_window_insets() {
        val zero = Insets.of(0, 0, 0, 0)
        val ascent = Insets.of(100, 200, 300, 400)
        val descent = Insets.of(400, 300, 200, 100)
        val united = Insets.max(ascent, descent)
        val windowInsets = ExtendedWindowInsets.Builder()
            .set(CustomType.testType1, zero)
            .set(CustomType.testType2, ascent)
            .set(CustomType.testType3, descent)
            .build()
        val actual1 = CustomType(windowInsets) { testType1 }
        val actual2 = windowInsets(CustomType) { testType2 }
        val actual3 = windowInsets { testType3 }
        val actual4 = windowInsets { testType1 or testType2 or testType3 }
        assertEquals(actual1, zero)
        assertEquals(actual2, ascent)
        assertEquals(actual3, descent)
        assertEquals(actual4, united)
    }

    @Test
    fun insets_value() {
        val input = Insets.of(100, 200, 300, 400)
        val actual = InsetsValue(input)
        assertEquals(actual.toInsets(), input)
    }
}

