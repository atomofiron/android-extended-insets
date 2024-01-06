package lib.atomofiron.insets

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding


class ViewInsetsDelegateImpl(
    private val view: View,
    dependency: Boolean,
    private val typeMask: Int = barsWithCutout,
) : ViewInsetsDelegate, InsetsListener {

    private var stockLeft = 0
    private var stockTop = 0
    private var stockRight = 0
    private var stockBottom = 0

    private var dstLeft = None
    private var dstTop = None
    private var dstRight = None
    private var dstBottom = None

    private var insets = Insets.NONE
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private var provider: InsetsProvider? = null
    private var listener: InsetsListener? = this

    init {
        view.onAttachCallback(
            onAttach = {
                provider = view.parent.findInsetsProvider()
                provider?.addInsetsListener(listener ?: return@onAttachCallback)
            },
            onDetach = {
                provider?.removeInsetsListener(listener ?: return@onAttachCallback)
                provider = null
            },
        )
        if (dependency) {
            view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if ((left != oldLeft || right != oldRight) && (dstLeft != None || dstRight != None)) {
                    provider?.requestInsets()
                } else if ((top != oldTop || bottom != oldBottom) && (dstTop != None || dstBottom != None)) {
                    provider?.requestInsets()
                }
            }
        }
    }

    override fun unsubscribeInsets(): ViewInsetsDelegate {
        provider?.removeInsetsListener(this)
        listener = null
        return this
    }

    override fun padding(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsDelegate {
        sync(
            left = if (start && !isRtl || end && isRtl) Padding else dstLeft,
            top = if (top) Padding else dstTop,
            right = if (start && isRtl || end && !isRtl) Padding else dstRight,
            bottom = if (bottom) Padding else dstBottom,
        )
        return this
    }

    override fun margin(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsDelegate {
        sync(
            left = if (start && !isRtl || end && isRtl) Margin else dstLeft,
            top = if (top) Margin else dstTop,
            right = if (start && isRtl || end && !isRtl) Margin else dstRight,
            bottom = if (bottom) Margin else dstBottom,
        )
        return this
    }

    override fun reset(): ViewInsetsDelegate {
        sync(None, None, None, None)
        return this
    }

    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) {
        // there is no need to turn into ExtendedWindowInsets
        insets = windowInsets.getInsets(typeMask)
        applyPadding(insets)
        applyMargin(insets)
    }

    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        insets = windowInsets.getInsets(typeMask)
        applyPadding(insets)
        applyMargin(insets)
    }

    private fun applyPadding(insets: Insets) {
        if (!syncAny(Padding)) return
        view.updatePadding(
            left = if (dstLeft == Padding) stockLeft + insets.left else view.paddingLeft,
            top = if (dstTop == Padding) stockTop + insets.top else view.paddingTop,
            right = if (dstRight == Padding) stockRight + insets.right else view.paddingRight,
            bottom = if (dstBottom == Padding) stockBottom + insets.bottom else view.paddingBottom,
        )
    }

    private fun applyMargin(insets: Insets) {
        if (!syncAny(Margin)) return
        view.updateLayoutParams<MarginLayoutParams> {
            leftMargin = if (dstLeft == Margin) stockLeft + insets.left else view.marginLeft
            topMargin = if (dstTop == Margin) stockTop + insets.top else view.marginTop
            rightMargin = if (dstRight == Margin) stockRight + insets.right else view.marginRight
            bottomMargin = if (dstBottom == Margin) stockBottom + insets.bottom else view.marginBottom
        }
    }

    private fun syncAny(dst: InsetsDestination): Boolean {
        return when (dst) {
            dstLeft -> true
            dstTop -> true
            dstRight -> true
            dstBottom -> true
            else -> false
        }
    }

    private fun sync(
        left: InsetsDestination,
        top: InsetsDestination,
        right: InsetsDestination,
        bottom: InsetsDestination,
    ) {
        if (syncAny(Padding)) applyPadding(Insets.NONE)
        if (syncAny(Margin)) applyMargin(Insets.NONE)
        dstLeft = left
        dstTop = top
        dstRight = right
        dstBottom = bottom
        stockLeft = if (left == Margin) view.marginLeft else view.paddingLeft
        stockTop = if (top == Margin) view.marginTop else view.paddingTop
        stockRight = if (right == Margin) view.marginRight else view.paddingRight
        stockBottom = if (bottom == Margin) view.marginBottom else view.paddingBottom
        applyPadding(insets)
        applyMargin(insets)
    }
}