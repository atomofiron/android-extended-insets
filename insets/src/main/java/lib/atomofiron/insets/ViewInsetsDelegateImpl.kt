/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package lib.atomofiron.insets

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import androidx.core.graphics.Insets
import androidx.core.view.ScrollingView
import androidx.core.view.isGone
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Translation
import kotlin.math.max

private val stubMarginLayoutParams = MarginLayoutParams(0, 0)

data class Source(
    val horizontal: Boolean,
    val vertical: Boolean,
    val callback: InsetsViewSourceCallback?,
) {
    companion object {
        val None = Source(horizontal = false, vertical = false, callback = null)
    }
    val any = horizontal || vertical
}

internal class ViewInsetsDelegateImpl(
    val view: View,
    override val types: TypeSet,
    private var combining: InsetsCombining? = null,
    dstStart: InsetsDestination = None,
    private var dstTop: InsetsDestination = None,
    dstEnd: InsetsDestination = None,
    private var dstBottom: InsetsDestination = None,
) : ViewInsetsDelegate, InsetsListener, InsetsSourceCallback, View.OnAttachStateChangeListener, View.OnLayoutChangeListener, ViewTreeObserver.OnGlobalLayoutListener {

    private val nameWithId: String = view.nameWithId()

    private var viewPlaced = !view.isGone
    private var insets = Insets.NONE
    private var windowInsets = ExtendedWindowInsets.Empty
    private var provider: InsetsProvider? = null
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private var postRequestLayoutOnNextLayout = false
    private var source = Source.None
    private var scrollOnEdge = false

    private var dstLeft = if (isRtl) dstEnd else dstStart
    private var dstRight = if (isRtl) dstStart else dstEnd

    init {
        logDestination("init")
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
        view.addOnLayoutChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        provider = view.findInsetsProvider()
        logd { "$nameWithId onAttach provider? ${provider != null}" }
        provider?.addInsetsListener(this)
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "$nameWithId onDetach provider? ${provider != null}" }
        provider?.removeInsetsListener(this)
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        provider = null
    }

    override fun onGlobalLayout() {
        val placed = !view.isGone
        if (placed != viewPlaced) {
            viewPlaced = placed
            if (source.any) {
                logd { "$nameWithId become placed? $placed, provider? ${provider != null}" }
                provider?.publishInsetsFrom(this)
            }
        }
    }

    override fun resetInsets(block: ViewInsetsConfig.() -> Unit): ViewInsetsDelegate {
        val config = ViewInsetsConfig().apply(block)
        config.logd { "$nameWithId with insets [${dstStart.label},${dstTop.label},${dstEnd.label},${dstBottom.label}]" }
        val delta = insets.inv()
        if (insets.isNotEmpty(Padding)) applyPadding(delta)
        if (insets.isNotEmpty(Margin)) applyMargin(delta)
        if (insets.isNotEmpty(Translation)) applyTranslation(delta)
        dstLeft = if (isRtl) config.dstEnd else config.dstStart
        dstTop = config.dstTop
        dstRight = if (isRtl) config.dstStart else config.dstEnd
        dstBottom = config.dstBottom
        logDestination("reset")
        updateInsets(windowInsets)
        return this
    }

    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        this.windowInsets = windowInsets
        updateInsets(windowInsets)
    }

    override fun source(horizontal: Boolean, vertical: Boolean, callback: InsetsViewSourceCallback?): ViewInsetsDelegate {
        source = Source(horizontal, vertical, callback)
        return this
    }

    override fun source(callback: InsetsViewSourceCallback?) = source(horizontal = true, vertical = true, callback)

    override fun scrollOnEdge(): ViewInsetsDelegate {
        scrollOnEdge = true
        return this
    }

    override fun combining(combining: InsetsCombining?) {
        this.combining = combining
    }

    // oldTop and oldBottom are sometimes wrong
    private val old = Rect()
    override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, ol: Int, ot: Int, or: Int, ob: Int) {
        val horizontally = (left != old.left || right != old.right) && (dstLeft != None || dstRight != None)
        val vertically = (top != old.top || bottom != old.bottom) && (dstTop != None || dstBottom != None)
        old.set(left, top, right, bottom)
        if (source.horizontal && horizontally || source.vertical && vertically) {
            logd { "$nameWithId modify insets? ${provider != null}" }
            provider?.publishInsetsFrom(this)
        }
        if (postRequestLayoutOnNextLayout) {
            postRequestLayoutOnNextLayout = false
            view.post { view.requestLayout() }
        }
    }

    override fun getSource(windowInsets: ExtendedWindowInsets): InsetsSource? {
        return source.callback?.getSource(InsetsCallbackArg(view, windowInsets))
    }

    private fun updateInsets(windowInsets: ExtendedWindowInsets) {
        val new = combining
            ?.combine(windowInsets)
            .let { it ?: windowInsets[types] }
            .filterUseful()
        if (insets != new) {
            val delta = Insets.subtract(new, insets)
            insets = new
            applyInsets(delta)
            if (isAnyMargin() && view.isGone) postRequestLayoutOnNextLayout = true
        }
    }

    private fun applyInsets(delta: Insets) {
        logInsets()
        applyPadding(delta)
        applyMargin(delta)
        applyTranslation(delta)
    }

    private fun applyPadding(delta: Insets) {
        if (delta.isEmpty(Padding)) return
        val scrollingPaddingTop = view.paddingTop.takeIf { scrollOnEdge }.takeIf {
            view is ScrollingView && (view as? ViewGroup)?.getChildAt(0)?.top == it
        }
        val scrollingPaddingBottom = view.paddingBottom.takeIf { scrollOnEdge }.takeIf {
            val bottomSpace = (view as? ViewGroup)
                ?.run { getChildAt(childCount.dec()) }
                ?.run { view.height - bottom - marginBottom }
            view is ScrollingView && bottomSpace == it
        }
        val oldPadding = view.takeIf { source.any }?.run {
            Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }
        val changed = view.updatePaddingIfChanged(
            left = view.paddingLeft + if (dstLeft == Padding) delta.left else 0,
            top = view.paddingTop + if (dstTop == Padding) delta.top else 0,
            right = view.paddingRight + if (dstRight == Padding) delta.right else 0,
            bottom = view.paddingBottom + if (dstBottom == Padding) delta.bottom else 0,
        )
        if (!changed) {
            return
        }
        scrollingPaddingTop?.let { old ->
            if (old < view.paddingTop) view.scrollBy(0, old - view.paddingTop)
        } ?: scrollingPaddingBottom?.let { old ->
            if (old < view.paddingBottom) view.scrollBy(0, view.paddingBottom - old)
        }
        oldPadding?.run {
            // trigger insets requesting
            if (source.horizontal) view.right += (view.paddingLeft + view.paddingRight) - (left + right)
            if (source.vertical) view.bottom += (view.paddingTop + view.paddingBottom) - (top + bottom)
        }
    }

    private fun applyMargin(delta: Insets) {
        if (delta.isEmpty(Margin)) return
        val params = getMarginLayoutParamsOrThrow()
        view.updateMarginIfChanged(
            params,
            left = params.leftMargin + if (dstLeft == Margin) delta.left else 0,
            top = params.topMargin + if (dstTop == Margin) delta.top else 0,
            right = params.rightMargin + if (dstRight == Margin) delta.right else 0,
            bottom = params.bottomMargin + if (dstBottom == Margin) delta.bottom else 0,
        )
    }

    private fun applyTranslation(delta: Insets) {
        if (delta.isEmpty(Translation)) return
        var dx = 0f
        var dy = 0f
        if (dstLeft == Translation) dx += delta.left
        if (dstRight == Translation) dx -= delta.right
        if (dstTop == Translation) dy += delta.top
        if (dstBottom == Translation) dy -= delta.bottom
        val request = source.horizontal && dx != 0f || source.vertical && dy != 0f
        view.translationX += dx
        view.translationY += dy
        if (request) {
            logd { "$nameWithId modify insets? ${provider != null}" }
            provider?.publishInsetsFrom(this)
        }
    }

    private fun Insets.isNotEmpty(dst: InsetsDestination) = !isEmpty(dst)

    private fun Insets.isEmpty(dst: InsetsDestination): Boolean = when {
        dst == dstLeft && left != 0 -> false
        dst == dstTop && top != 0 -> false
        dst == dstRight && right != 0 -> false
        dst == dstBottom && bottom != 0 -> false
        else -> true
    }

    private fun isAnyMargin(): Boolean {
        return when (Margin) {
            dstLeft -> true
            dstTop -> true
            dstRight -> true
            dstBottom -> true
            else -> false
        }
    }

    private fun getMarginLayoutParamsOrStub(): MarginLayoutParams {
        return (view.layoutParams as? MarginLayoutParams) ?: stubMarginLayoutParams
    }

    private fun getMarginLayoutParamsOrThrow(): MarginLayoutParams {
        return (view.layoutParams as? MarginLayoutParams) ?: throw NoMarginLayoutParams(nameWithId)
    }

    private fun Insets.filterUseful(): Insets {
        return Insets.of(
            if (dstLeft.isNone) 0 else left,
            if (dstTop.isNone) 0 else top,
            if (dstRight.isNone) 0 else right,
            if (dstBottom.isNone) 0 else bottom,
        )
    }

    private fun View.updatePaddingIfChanged(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        when {
            paddingLeft != left -> Unit
            paddingTop != top -> Unit
            paddingRight != right -> Unit
            paddingBottom != bottom -> Unit
            else -> return false
        }
        updatePadding(left, top, right, bottom)
        return true
    }

    private fun View.updateMarginIfChanged(params: MarginLayoutParams, left: Int, top: Int, right: Int, bottom: Int) {
        when {
            params.leftMargin != left -> Unit
            params.topMargin != top -> Unit
            params.rightMargin != right -> Unit
            params.bottomMargin != bottom -> Unit
            else -> return
        }
        updateLayoutParams<MarginLayoutParams> {
            leftMargin = left
            topMargin = top
            rightMargin = right
            bottomMargin = bottom
        }
    }

    private fun InsetsCombining.combine(windowInsets: ExtendedWindowInsets): Insets {
        val params = getMarginLayoutParamsOrStub()
        val stockLeft = when (dstLeft) {
            Margin -> params.leftMargin - insets.left
            Padding -> view.paddingLeft - insets.left
            Translation -> params.leftMargin
            else -> 0
        }
        val stockTop = when (dstTop) {
            Margin -> params.topMargin - insets.top
            Padding -> view.paddingTop - insets.top
            Translation -> params.topMargin
            else -> 0
        }
        val stockRight = when (dstRight) {
            Margin -> params.rightMargin - insets.right
            Padding -> view.paddingRight - insets.right
            Translation -> params.rightMargin
            else -> 0
        }
        val stockBottom = when (dstBottom) {
            Margin -> params.bottomMargin - insets.bottom
            Padding -> view.paddingBottom - insets.bottom
            Translation -> params.bottomMargin
            else -> 0
        }
        val stock = Insets.of(
            max(stockLeft, if (isRtl) minEnd else minStart),
            max(stockTop, minTop),
            max(stockRight, if (isRtl) minStart else minEnd),
            max(stockBottom, minBottom),
        )
        if (stock.isEmpty()) {
            return windowInsets[types]
        }
        val intersection = combiningTypes * types
        val other = windowInsets[types - intersection]
        if (intersection.isEmpty()) {
            return other
        }
        val space = windowInsets[intersection]
        if (space.isEmpty()) {
            return other
        }
        val subtracted = Insets.subtract(space, stock)
        return Insets.max(other, subtracted)
    }

    private fun logDestination(action: String) {
        logd {
            val list = listOfNotNull(
                (if (isRtl) dstRight else dstLeft).takeIf { !it.isNone }?.let { "start=$it" },
                dstTop.takeIf { !it.isNone }?.let { "top=$it" },
                (if (isRtl) dstLeft else dstRight).takeIf { !it.isNone }?.let { "end=$it" },
                dstBottom.takeIf { !it.isNone }?.let { "bottom=$it" },
            ).joinToString(", ")
            "$nameWithId $action insets $types, ${list.ifEmpty { None.toString() }}"
        }
    }

    private fun logInsets() {
        logd {
            val left = if (dstLeft.isNone) "" else insets.left.toString()
            val top = if (dstTop.isNone) "" else insets.top.toString()
            val right = if (dstRight.isNone) "" else insets.right.toString()
            val bottom = if (dstBottom.isNone) "" else insets.bottom.toString()
            val types = types.getTypes(windowInsets, !dstLeft.isNone, !dstTop.isNone, !dstRight.isNone, !dstBottom.isNone)
            "$nameWithId insets[${dstLeft.letter}$left,${dstTop.letter}$top,${dstRight.letter}$right,${dstBottom.letter}$bottom] $types"
        }
    }

    override fun toString(): String = "ViewInsetsDelegateImpl(${nameWithId})"
}