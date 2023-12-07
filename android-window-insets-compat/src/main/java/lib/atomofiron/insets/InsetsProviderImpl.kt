package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.CopyOnWriteArraySet


private data class SrcState(
    val windowInsets: WindowInsetsCompat = WindowInsetsCompat.CONSUMED,
    val hasModifier: Boolean = false,
    val hasListeners: Boolean = false,
)

class InsetsProviderImpl : InsetsProvider, InsetsListener {

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
    private val listeners = CopyOnWriteArraySet<InsetsListener>()
    private var insetsModifier: InsetsModifier? = null
    private var provider: InsetsProvider? = null
    private var thisView: View? = null

    override fun onInit(thisView: View) {
        this.thisView = thisView
        thisView.onAttachCallback(
            onAttach = {
                provider = thisView.parent.getInsetsProvider()
                provider?.addListener(this)
            },
            onDetach = {
                provider?.removeListener(this)
                provider = null
            },
        )
    }

    override fun setInsetsModifier(modifier: InsetsModifier) {
        insetsModifier = modifier
        srcState = srcState.copy(hasModifier = true)
    }

    override fun addListener(listener: InsetsListener) {
        srcState = srcState.copy(hasListeners = true)
        listeners.add(listener)
        listener.onApplyWindowInsets(current)
    }

    override fun removeListener(listener: InsetsListener) {
        if (listeners.remove(listener)) {
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
            it.onApplyWindowInsets(windowInsets)
        }
    }
}
