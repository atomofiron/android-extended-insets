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
        updateOriginal(start, top, end, bottom)
        applyPadding()
    }

    override fun updateMarginRelative(start: Int, top: Int, end: Int, bottom: Int) {
        updateOriginal(start, top, end, bottom)
        applyMargin()
    }

    override fun apply(windowInsets: WindowInsetsCompat) {
        insets = windowInsets.getInsets(typeMask)
        when (destination) {
            Padding -> applyPadding()
            Margin -> applyMargin()
        }
    }

    private fun applyPadding() {
        val leftInset = originalLeft + if (left) insets.left else 0
        val topInset = originalTop + if (top) insets.top else 0
        val rightInset = originalRight + if (right) insets.right else 0
        val bottomInset = originalBottom + if (bottom) insets.bottom else 0
        val topDif = view.paddingTop - topInset
        val bottomDif = view.paddingBottom - bottomInset
        val scrollDif = when {
            destination != Padding -> 0
            topDif == 0 && bottomDif == 0 -> 0
            else -> 0
            /*view !is RecyclerView -> 0
            view.scrollState != RecyclerView.SCROLL_STATE_IDLE -> 0
            view.getLastItemView()?.let { it.bottom <= (view.height - view.paddingBottom) } == true -> -bottomDif
            else -> topDif*/
        }
        view.updatePadding(left = leftInset, top = topInset, right = rightInset, bottom = bottomInset)
        view.scrollBy(0, scrollDif)
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

/*private fun RecyclerView.getLastItemView(): View? {
    val adapter = adapter ?: return null
    return findViewHolderForAdapterPosition(adapter.itemCount.dec())?.itemView
}

private fun RecyclerView.getFirstItemView(): View? {
    return findViewHolderForAdapterPosition(0)?.itemView
}*/
