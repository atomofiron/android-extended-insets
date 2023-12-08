package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat


private const val INVALID_INSETS_LISTENER_KEY = 0

private data class SrcState(
    val windowInsets: WindowInsetsCompat = WindowInsetsCompat.CONSUMED,
    val hasModifier: Boolean = false,
    val hasListeners: Boolean = false,
)

class InsetsProviderImpl : InsetsProvider {

    private var srcState = SrcState()
        set(value) {
            field = value
            updateCurrent(value)
        }
    override var current = WindowInsetsCompat.CONSUMED
        private set(value) {
            field = value
            notifyListeners(value)
        }
    private val listeners = hashMapOf<Int, InsetsListener>()
    private var insetsModifier: InsetsModifier? = null
    private var provider: InsetsProvider? = null
    private var thisView: View? = null
    private var nextKey = INVALID_INSETS_LISTENER_KEY.inc()

    override fun onInit(thisView: View) {
        this.thisView = thisView
        thisView.onAttachCallback(
            onAttach = {
                provider = thisView.parent.getInsetsProvider()
                provider?.addInsetsListener(this)
            },
            onDetach = {
                provider?.removeInsetsListener(this)
                provider = null
            },
        )
    }

    override fun setInsetsModifier(modifier: InsetsModifier) {
        insetsModifier = modifier
        srcState = srcState.copy(hasModifier = true)
    }

    override fun addInsetsListener(listener: InsetsListener): Int {
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
            srcState = srcState.copy(windowInsets = windowInsetsCompat)
        }
        return windowInsets
    }

    // the one of the two entry points for system window insets
    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) {
        srcState = srcState.copy(windowInsets = windowInsets)
    }

    private fun updateCurrent(srcState: SrcState) {
        current = insetsModifier?.getInsets(listeners.isNotEmpty(), srcState.windowInsets) ?: srcState.windowInsets
    }

    private fun notifyListeners(windowInsets: WindowInsetsCompat) {
        listeners.forEach {
            it.value.onApplyWindowInsets(windowInsets)
        }
    }
}
