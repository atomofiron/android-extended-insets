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
import androidx.core.view.updateMarginsRelative
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.Padding


class ViewInsetsDelegateImpl(
    private val view: View,
    start: Boolean,
    private val top: Boolean,
    end: Boolean,
    private val bottom: Boolean,
    private val destination: InsetsDestination,
    private val provider: InsetsProvider?,
    private val typeMask: Int = barsWithCutout,
) : ViewInsetsDelegate, InsetsListener {

    private val left = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) end else start
    private val right = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) start else end

    private var originalLeft = if (destination == Padding) view.paddingLeft else view.marginLeft
    private var originalTop =  if (destination == Padding) view.paddingTop else view.marginTop
    private var originalRight =  if (destination == Padding) view.paddingRight else view.marginRight
    private var originalBottom =  if (destination == Padding) view.paddingBottom else view.marginBottom

    private var insets = Insets.NONE

    init {
        if (provider != null) {
            view.onAttachCallback(
                onAttach = { provider.addListener(this) },
                onDetach = { provider.removeListener(this) },
            )
        }
    }

    override fun onApplyWindowInsets(windowInsets: WindowInsetsCompat) = apply(windowInsets)

    override fun updatePaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        if (destination == Padding) {
            updateOriginal(start, top, end, bottom)
            applyPadding()
        } else {
            view.updateLayoutParams<MarginLayoutParams> {
                updateMarginsRelative(start, top, end, bottom)
            }
        }
    }

    override fun updateMarginRelative(start: Int, top: Int, end: Int, bottom: Int) {
        if (destination == Margin) {
            updateOriginal(start, top, end, bottom)
            applyMargin()
        } else {
            view.updatePaddingRelative(start, top, end, bottom)
        }
    }

    override fun apply(windowInsets: WindowInsetsCompat) {
        insets = windowInsets.getInsets(typeMask)
        when (destination) {
            Padding -> applyPadding()
            Margin -> applyMargin()
        }
    }

    private fun applyPadding() {
        val left = originalLeft + if (left) insets.left else 0
        val top = originalTop + if (top) insets.top else 0
        val right = originalRight + if (right) insets.right else 0
        val bottom = originalBottom + if (bottom) insets.bottom else 0
        view.updatePadding(left, top, right, bottom)
    }

    private fun applyMargin() {
        view.updateLayoutParams<MarginLayoutParams> {
            leftMargin = originalLeft + if (left) insets.left else 0
            topMargin = originalTop + if (top) insets.top else 0
            rightMargin = originalRight + if (right) insets.right else 0
            bottomMargin = originalBottom + if (bottom) insets.bottom else 0
        }
    }

    private fun updateOriginal(start: Int, top: Int, end: Int, bottom: Int) {
        originalLeft = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) end else start
        originalTop = top
        originalRight = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) start else end
        originalBottom = bottom
    }
}