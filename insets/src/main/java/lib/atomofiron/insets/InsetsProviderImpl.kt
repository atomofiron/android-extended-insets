package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat


const val INVALID_INSETS_LISTENER_KEY = 0

private data class SrcState(
    val windowInsets: ExtendedWindowInsets = ExtendedWindowInsets.CONSUMED,
    val hasModifier: Boolean = false,
    val hasListeners: Boolean = false,
)

class InsetsProviderImpl : InsetsProvider {

    private var srcState = SrcState()
        set(value) {
            logd { "${thisView?.nameWithId()} new state? ${field != value}" }
            if (value.hasModifier || field != value) {
                field = value
                updateCurrent(value)
            }
        }
    override var current = ExtendedWindowInsets.CONSUMED
        private set(value) {
            logd { "${thisView?.nameWithId()} new current? ${field != value}" }
            if (field != value) {
                field = value
                notifyListeners(value)
            }
        }
    private val listeners = hashMapOf<Int, InsetsListener>()
    private var insetsModifier: InsetsModifier? = null
    private var provider: InsetsProvider? = null
    private var thisView: View? = null
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()

    override fun View.onInit() {
        thisView = this
        onAttachCallback(
            onAttach = {
                provider = parent.findInsetsProvider()
                provider?.addInsetsListener(this@InsetsProviderImpl)
            },
            onDetach = {
                provider?.removeInsetsListener(this@InsetsProviderImpl)
                provider = null
            },
        )
    }

    override fun setInsetsModifier(modifier: InsetsModifier) {
        logd { "${thisView?.nameWithId()} set modifier" }
        insetsModifier = modifier
        srcState = srcState.copy(hasModifier = true)
    }

    override fun addInsetsListener(listener: InsetsListener): Int {
        if (listener === thisView || listener === this) {
            throw IllegalArgumentException()
        }
        logd { "${thisView?.nameWithId()} set modifier" }
        listeners.entries
            .find { it.value === listener }
            ?.let { return it.key }
        val key = nextKey++
        srcState = srcState.copy(hasListeners = true)
        listeners[key] = listener
        listener.onApplyWindowInsets(current)
        return key
    }

    override fun removeInsetsListener(listener: InsetsListener) {
        val entry = listeners.entries.find { it.value === listener }
        entry ?: return
        removeInsetsListener(entry.key)
    }

    override fun removeInsetsListener(key: Int) {
        if (listeners.remove(key) != null) {
            srcState = srcState.copy(hasListeners = listeners.isNotEmpty())
        }
    }

    // the one of the two entry points for system window insets
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (provider == null) {
            val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, thisView)
            srcState = srcState.copy(windowInsets = windowInsetsCompat.toExtended())
        }
        return windowInsets
    }

    override fun requestInsets() = updateCurrent(srcState)

    // the one of the two entry points for system window insets
    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        srcState = srcState.copy(windowInsets = windowInsets)
    }

    private fun updateCurrent(srcState: SrcState) {
        logd { "${thisView?.nameWithId()} update current with modifier? ${insetsModifier != null}" }
        current = insetsModifier
            ?.transform(listeners.isNotEmpty(), srcState.windowInsets)
            ?.toExtended()
            ?: srcState.windowInsets
    }

    private fun notifyListeners(windowInsets: ExtendedWindowInsets) {
        listeners.forEach {
            it.value.onApplyWindowInsets(windowInsets)
        }
    }
}
