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
import lib.atomofiron.insets.InsetsDestination.Padding


class ViewInsetsDelegateImpl(
    private val view: View,
    private val typeMask: Int = barsWithCutout,
) : ViewInsetsDelegate, InsetsListener {

    private var stockLeft = 0
    private var stockTop = 0
    private var stockRight = 0
    private var stockBottom = 0

    private var dstLeft = InsetsDestination.None
    private var dstTop = InsetsDestination.None
    private var dstRight = InsetsDestination.None
    private var dstBottom = InsetsDestination.None

    private var insets = Insets.NONE
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private var provider: InsetsProvider? = null
    private var attachListener: View.OnAttachStateChangeListener? = null

    init {
        attachListener = view.onAttachCallback(
            onAttach = {
                provider = view.parent.findInsetsProvider()
                provider?.addInsetsListener(this)
            },
            onDetach = {
                provider?.removeInsetsListener(this)
                provider = null
            },
        )
    }

    override fun detachInsetsProvider() {
        provider?.removeInsetsListener(this)
        view.removeOnAttachStateChangeListener(attachListener)
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

    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) {
        insets = windowInsets.getInsets(typeMask)
        applyPadding()
        applyMargin()
    }

    private fun applyPadding() {
        if (!syncAny(Padding)) return
        view.updatePadding(
            left = if (dstLeft == Padding) stockLeft + insets.left else view.paddingLeft,
            top = if (dstTop == Padding) stockTop + insets.top else view.paddingTop,
            right = if (dstRight == Padding) stockRight + insets.right else view.paddingRight,
            bottom = if (dstBottom == Padding) stockBottom + insets.bottom else view.paddingBottom,
        )
    }

    private fun applyMargin() {
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
        dstLeft = left
        dstTop = top
        dstRight = right
        dstBottom = bottom
        stockLeft = if (left == Margin) view.marginLeft else view.paddingLeft
        stockTop = if (top == Margin) view.marginTop else view.paddingTop
        stockRight = if (right == Margin) view.marginRight else view.paddingRight
        stockBottom = if (bottom == Margin) view.marginBottom else view.paddingBottom
        applyPadding()
        applyMargin()
    }
}