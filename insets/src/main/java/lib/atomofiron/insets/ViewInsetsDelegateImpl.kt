/*
 * Copyright 2024 Yaroslav Nesterov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.atomofiron.insets

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.ScrollingView
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.barsWithCutout
import lib.atomofiron.insets.InsetsDestination.Margin
import lib.atomofiron.insets.InsetsDestination.None
import lib.atomofiron.insets.InsetsDestination.Padding
import lib.atomofiron.insets.InsetsDestination.Translation
import kotlin.math.max

private val stubMarginLayoutParams = MarginLayoutParams(0, 0)

data class Dependency(
    val horizontal: Boolean = false,
    val vertical: Boolean = false,
) {
    val any = horizontal || vertical
}

internal class ViewInsetsDelegateImpl(
    internal val view: View,
    private val types: TypeSet = barsWithCutout,
    private var combining: InsetsCombining? = null,
    dstStart: InsetsDestination = None,
    private var dstTop: InsetsDestination = None,
    dstEnd: InsetsDestination = None,
    private var dstBottom: InsetsDestination = None,
) : ViewInsetsDelegate, InsetsListener, View.OnAttachStateChangeListener, View.OnLayoutChangeListener {

    private val nameWithId = view.nameWithId()
    override var consuming: TypeSet = TypeSet.EMPTY
        private set

    private var stockLeft = 0
    private var stockTop = 0
    private var stockRight = 0
    private var stockBottom = 0

    private var insets = Insets.NONE
    private var windowInsets = ExtendedWindowInsets.EMPTY
    private var provider: InsetsProvider? = null
    private var listener: InsetsListener? = this
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private var postRequestLayoutOnNextLayout = false
    private var dependency = Dependency()
    private var scrollOnEdge = false

    private var dstLeft = if (isRtl) dstEnd else dstStart
    private var dstRight = if (isRtl) dstStart else dstEnd

    init {
        saveStock()
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
        view.addOnLayoutChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        provider = view.parent.findInsetsProvider()
        logd { "$nameWithId onAttach provider? ${provider != null}, listener? ${listener != null}" }
        provider?.addInsetsListener(listener ?: return)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "$nameWithId onDetach provider? ${provider != null}, listener? ${listener != null}" }
        provider?.removeInsetsListener(listener ?: return)
        provider = null
    }

    override fun detachFromProvider() {
        logd { "$nameWithId unsubscribe insets? ${provider != null}, listener? ${listener != null}" }
        provider?.removeInsetsListener(listener ?: return)
        listener = null
    }

    override fun detachFromView() {
        logd { "$nameWithId detach? idk" }
        detachFromProvider()
        view.removeOnLayoutChangeListener(this)
        view.removeOnAttachStateChangeListener(this)
    }

    override fun resetInsets(consuming: TypeSet, block: ViewInsetsConfig.() -> Unit): ViewInsetsDelegate {
        val config = ViewInsetsConfig().apply(block)
        config.logd { "$nameWithId with insets [${dstStart.label},${dstTop.label},${dstEnd.label},${dstBottom.label}]" }
        if (isAny(Padding) && insets.isNotEmpty()) applyPadding(Insets.NONE)
        if (isAny(Margin) && insets.isNotEmpty()) applyMargin(Insets.NONE)
        if (isAny(Translation) && insets.isNotEmpty()) applyTranslation(Insets.NONE)
        dstLeft = if (isRtl) config.dstEnd else config.dstStart
        dstTop = config.dstTop
        dstRight = if (isRtl) config.dstStart else config.dstEnd
        dstBottom = config.dstBottom
        saveStock()
        consuming(consuming)
        updateInsets(windowInsets)
        applyInsets()
        return this
    }

    override fun onApplyWindowInsets(windowInsets: ExtendedWindowInsets) {
        this.windowInsets = windowInsets
        updateInsets(windowInsets)
    }

    override fun dependency(horizontal: Boolean, vertical: Boolean): ViewInsetsDelegate {
        dependency = dependency.copy(horizontal = horizontal, vertical = vertical)
        return this
    }

    override fun scrollOnEdge(): ViewInsetsDelegate {
        scrollOnEdge = true
        return this
    }

    override fun consuming(types: TypeSet): ViewInsetsDelegate {
        consuming = types
        return this
    }

    override fun combining(combining: InsetsCombining?) {
        this.combining = combining
    }

    override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int, ) {
        val horizontally = (left != oldLeft || right != oldRight) && (dstLeft != None || dstRight != None)
        val vertically = (top != oldTop || bottom != oldBottom) && (dstTop != None || dstBottom != None)
        if (dependency.horizontal && horizontally || dependency.vertical && vertically) {
            logd { "$nameWithId request insets? ${provider != null}" }
            provider?.requestInsets()
        }
        if (postRequestLayoutOnNextLayout) {
            postRequestLayoutOnNextLayout = false
            view.post { view.requestLayout() }
        }
    }

    private fun saveStock() {
        val params = when {
            isAny(Margin) -> getMarginLayoutParamsOrThrow()
            // margin useful on translation to combine with insets
            isAny(Translation) -> getMarginLayoutParamsOrStub()
            else -> stubMarginLayoutParams
        }
        stockLeft = if (dstLeft == Padding) view.paddingLeft else params.leftMargin
        stockTop = if (dstTop == Padding) view.paddingTop else params.topMargin
        stockRight = if (dstRight == Padding) view.paddingRight else params.rightMargin
        stockBottom = if (dstBottom == Padding) view.paddingBottom else params.bottomMargin
    }

    private fun updateInsets(windowInsets: ExtendedWindowInsets) {
        val new = (combining?.combine(windowInsets) ?: windowInsets[types])
            .filterUseful()
            .consume(consuming, windowInsets)
        if (insets != new) {
            insets = new
            applyInsets()
            if (isAny(Margin) && !view.isShown) postRequestLayoutOnNextLayout = true
        }
    }

    private fun applyInsets() {
        logInsets()
        applyPadding(insets)
        applyMargin(insets)
        applyTranslation(insets)
    }

    private fun applyPadding(insets: Insets) {
        if (!isAny(Padding)) return
        val scrollingPaddingTop = view.paddingTop.takeIf { scrollOnEdge }.takeIf {
            view is ScrollingView && (view as? ViewGroup)?.getChildAt(0)?.top == it
        }
        val scrollingPaddingBottom = view.paddingBottom.takeIf { scrollOnEdge }.takeIf {
            val bottomSpace = (view as? ViewGroup)
                ?.run { getChildAt(childCount.dec()) }
                ?.run { view.height - bottom - marginBottom }
            view is ScrollingView && bottomSpace == it
        }
        val oldPadding = view.takeIf { dependency.any }?.run {
            Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }
        val changed = view.updatePaddingIfChanged(
            left = if (dstLeft == Padding) stockLeft + insets.left else view.paddingLeft,
            top = if (dstTop == Padding) stockTop + insets.top else view.paddingTop,
            right = if (dstRight == Padding) stockRight + insets.right else view.paddingRight,
            bottom = if (dstBottom == Padding) stockBottom + insets.bottom else view.paddingBottom,
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
            if (dependency.horizontal) view.right += (view.paddingLeft + view.paddingRight) - (left + right)
            if (dependency.vertical) view.bottom += (view.paddingTop + view.paddingBottom) - (top + bottom)
        }
    }

    private fun applyMargin(insets: Insets) {
        if (!isAny(Margin)) return
        val params = getMarginLayoutParamsOrThrow()
        view.updateMarginIfChanged(
            params,
            left = if (dstLeft == Margin) stockLeft + insets.left else params.leftMargin,
            top = if (dstTop == Margin) stockTop + insets.top else params.topMargin,
            right = if (dstRight == Margin) stockRight + insets.right else params.rightMargin,
            bottom = if (dstBottom == Margin) stockBottom + insets.bottom else params.bottomMargin,
        )
    }

    private fun applyTranslation(insets: Insets) {
        if (!isAny(Translation)) return
        var dx = 0
        var dy = 0
        if (dstLeft == Translation) dx += insets.left
        if (dstRight == Translation) dx -= insets.right
        if (dstTop == Translation) dy += insets.top
        if (dstBottom == Translation) dy -= insets.bottom
        view.translationX = dx.toFloat()
        view.translationY = dy.toFloat()
    }

    private fun isAny(dst: InsetsDestination): Boolean {
        return when (dst) {
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

    private fun Insets.consume(consuming: TypeSet, windowInsets: ExtendedWindowInsets): Insets {
        if (consuming.isEmpty()) return this
        val insets = windowInsets[consuming]
        if (insets.isEmpty()) return this
        return consume(insets)
    }

    private fun InsetsCombining.combine(windowInsets: ExtendedWindowInsets): Insets {
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
}