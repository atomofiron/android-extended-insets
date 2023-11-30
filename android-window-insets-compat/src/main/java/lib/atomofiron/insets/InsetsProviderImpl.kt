package lib.atomofiron.insets

import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import lib.atomofiron.insets.onAttachCallback
import java.util.concurrent.CopyOnWriteArraySet

class InsetsProviderImpl : InsetsProvider, InsetsListener {

    private val listeners = CopyOnWriteArraySet<InsetsListener>()
    private var current = WindowInsetsCompat.CONSUMED
    private var insetsModifier: InsetsModifier? = null
    private var thisView: View? = null
    private var provider: InsetsProvider? = null

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
    }

    override fun addListener(listener: InsetsListener) {
        listeners.add(listener)
        listener.onApplyWindowInsets(current)
    }

    override fun removeListener(listener: InsetsListener) {
        listeners.remove(listener)
    }

    // the one of the two entry points for system window insets
    override fun dispatchApplyWindowInsets(windowInsets: WindowInsets): WindowInsets {
        if (provider == null) {
            onApplyWindowInsets(WindowInsetsCompat.toWindowInsetsCompat(windowInsets, thisView))
        }
        return windowInsets
    }

    // the one of the two entry points for system window insets
    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) {
        current = insetsModifier?.getInsets(windowInsets) ?: windowInsets
        listeners.forEach {
            it.onApplyWindowInsets(current)
        }
    }
}
