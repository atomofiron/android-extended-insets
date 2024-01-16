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
    private var windowInsets = WindowInsetsCompat.CONSUMED
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private var provider: InsetsProvider? = null
    private var listener: InsetsListener? = this

    init {
        view.onAttachCallback(
            onAttach = {
                provider = view.parent.findInsetsProvider()
                logd { "${view.nameWithId()} onAttach provider? ${provider != null}" }
                provider?.addInsetsListener(listener ?: return@onAttachCallback)
            },
            onDetach = {
                logd { "${view.nameWithId()} onDetach provider? ${provider != null}" }
                provider?.removeInsetsListener(listener ?: return@onAttachCallback)
                provider = null
            },
        )
        if (dependency) {
            view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val horizontally = (left != oldLeft || right != oldRight) && (dstLeft != None || dstRight != None)
                val vertically = (top != oldTop || bottom != oldBottom) && (dstTop != None || dstBottom != None)
                if (horizontally || vertically) {
                    logd { "${view.nameWithId()} request insets? ${provider != null}" }
                    provider?.requestInsets()
                }
            }
        }
    }

    override fun unsubscribeInsets(): ViewInsetsDelegate {
        logd { "${view.nameWithId()} unsubscribe insets? ${provider != null}" }
        provider?.removeInsetsListener(this)
        listener = null
        return this
    }

    override fun padding(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsDelegate {
        logDestinations("padding", start, top, end, bottom)
        sync(
            left = if (start && !isRtl || end && isRtl) Padding else dstLeft,
            top = if (top) Padding else dstTop,
            right = if (start && isRtl || end && !isRtl) Padding else dstRight,
            bottom = if (bottom) Padding else dstBottom,
        )
        return this
    }

    override fun margin(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean): ViewInsetsDelegate {
        logDestinations("margin", start, top, end, bottom)
        sync(
            left = if (start && !isRtl || end && isRtl) Margin else dstLeft,
            top = if (top) Margin else dstTop,
            right = if (start && isRtl || end && !isRtl) Margin else dstRight,
            bottom = if (bottom) Margin else dstBottom,
        )
        return this
    }

    override fun reset(): ViewInsetsDelegate {
        logd { "${view.nameWithId()} reset" }
        sync(None, None, None, None)
        return this
    }

    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) {
        // there is no need to turn into ExtendedWindowInsets
        this.windowInsets = windowInsets
        insets = windowInsets.getInsets(typeMask)
        logInsets()
        applyPadding(insets)
        applyMargin(insets)
    }

    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        this.windowInsets = windowInsets
        insets = windowInsets.getInsets(typeMask)
        logInsets()
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
        logInsets()
        applyPadding(insets)
        applyMargin(insets)
    }

    private fun logDestinations(destination: String, start: Boolean, top: Boolean, end: Boolean, bottom: Boolean) {
        logd {
            val sides = mutableListOf<String>().apply {
                if (start) add("start")
                if (top) add("top")
                if (end) add("end")
                if (bottom) add("bottom")
            }.joinToString()
            "${view.nameWithId()} +sync $destination: $sides"
        }
    }

    private fun logInsets() {
        if (!debugInsets) return
        val left = if (dstLeft.isNone) "" else insets.left.toString()
        val top = if (dstTop.isNone) "" else insets.top.toString()
        val right = if (dstRight.isNone) "" else insets.right.toString()
        val bottom = if (dstBottom.isNone) "" else insets.bottom.toString()
        val types = typeMask.getTypes(windowInsets, !dstLeft.isNone, !dstTop.isNone, !dstRight.isNone, !dstBottom.isNone)
        logd { "${view.nameWithId()} ${dstLeft.letter}$left ${dstTop.letter}$top ${dstRight.letter}$right ${dstBottom.letter}$bottom, $types" }
    }
}