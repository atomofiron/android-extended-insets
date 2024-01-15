package lib.atomofiron.insets

import android.view.View
import android.view.ViewParent
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type


val barsWithCutout: Int = Type.systemBars() or Type.displayCutout()

fun WindowInsetsCompat.displayCutout(): Insets = getInsets(Type.displayCutout())

fun WindowInsetsCompat.systemBars(): Insets = getInsets(Type.systemBars())

fun WindowInsetsCompat.barsWithCutout(): Insets = getInsets(barsWithCutout)

fun WindowInsetsCompat.isEmpty(typeMask: Int): Boolean = getInsets(typeMask).isEmpty()

fun Insets.isEmpty() = this == Insets.NONE

fun ViewParent.findInsetsProvider(): InsetsProvider? {
    return (this as? InsetsProvider) ?: parent?.findInsetsProvider()
}

fun View.addInsetsListener(listener: InsetsListener): Int {
    val provider = (this as? InsetsProvider) ?: parent.findInsetsProvider()
    val key = provider?.addInsetsListener(listener)
    key ?: logd { "${nameWithId()} insets provider not found" }
    return key ?: INVALID_INSETS_LISTENER_KEY
}

fun View.syncInsets(
    typeMask: Int = barsWithCutout,
    dependency: Boolean = false,
): ViewInsetsDelegate = ViewInsetsDelegateImpl(this, dependency, typeMask)

inline fun InsetsProvider.composeInsets(
    // these receive original insets (from parent provider or stock system window insets)
    vararg delegates: ViewInsetsDelegate,
    crossinline transformation: (hasListeners: Boolean, ExtendedWindowInsets) -> WindowInsetsCompat,
) {
    delegates.forEach { it.unsubscribeInsets() }
    setInsetsModifier { hasListeners, windowInsets ->
        delegates.forEach { it.onApplyWindowInsets(windowInsets) }
        transformation(hasListeners, windowInsets).toExtended()
    }
}

fun WindowInsetsCompat.toExtended() = when (this) {
    is ExtendedWindowInsets -> this
    else -> ExtendedWindowInsets(this)
}

inline fun <T : ExtendedWindowInsets.Type> T.from(
    windowInsets: WindowInsetsCompat,
    block: T.() -> Int,
): Insets {
    return windowInsets.getInsets(block())
}

inline operator fun <T : ExtendedWindowInsets.Type> WindowInsetsCompat.invoke(
    companion: T,
    block: T.() -> Int,
): Insets {
    return getInsets(companion.block())
}

operator fun WindowInsetsCompat.get(type: Int): Insets = getInsets(type)

fun View.onAttachCallback(
    onAttach: (View) -> Unit,
    onDetach: (View) -> Unit,
): View.OnAttachStateChangeListener {
    if (isAttachedToWindow) onAttach(this)
    return object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = onAttach(view)
        override fun onViewDetachedFromWindow(view: View) = onDetach(view)
    }.also { addOnAttachStateChangeListener(it) }
}
