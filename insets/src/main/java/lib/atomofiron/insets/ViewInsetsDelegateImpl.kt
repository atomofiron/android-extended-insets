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


internal class ViewInsetsDelegateImpl(
    internal val view: View,
    dependency: Boolean,
    private val typeMask: Int = barsWithCutout,
    dstStart: InsetsDestination = None,
    private var dstTop: InsetsDestination = None,
    dstEnd: InsetsDestination = None,
    private var dstBottom: InsetsDestination = None,
) : ViewInsetsDelegate, InsetsListener {

    private var stockLeft = 0
    private var stockTop = 0
    private var stockRight = 0
    private var stockBottom = 0

    private var insets = Insets.NONE
    private var windowInsets = WindowInsetsCompat.CONSUMED
    private var provider: InsetsProvider? = null
    private var listener: InsetsListener? = this
    private val isRtl: Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL

    private var dstLeft = if (isRtl) dstEnd else dstStart
    private var dstRight = if (isRtl) dstStart else dstEnd

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

    override fun withInsets(block: ViewInsetsConfig.() -> Unit): ViewInsetsDelegate {
        val config = ViewInsetsConfigImpl().apply(block)
        withInsets(config.dstStart, config.dstTop, config.dstEnd, config.dstBottom)
        return this
    }

    override fun withInsets(start: InsetsDestination?, top: InsetsDestination?, end: InsetsDestination?, bottom: InsetsDestination?): ViewInsetsDelegate {
        if (isAny(Padding)) applyPadding(Insets.NONE)
        if (isAny(Margin)) applyMargin(Insets.NONE)
        dstLeft = (if (isRtl) end else start) ?: dstLeft
        dstTop = top ?: dstTop
        dstRight = (if (isRtl) start else end) ?: dstRight
        dstBottom = bottom ?: dstBottom
        logd { "${view.nameWithId()} with insets [${dstLeft.label},${dstTop.label},${dstRight.label},${dstBottom.label}]" }
        stockLeft = if (dstLeft == Margin) view.marginLeft else view.paddingLeft
        stockTop = if (dstTop == Margin) view.marginTop else view.paddingTop
        stockRight = if (dstRight == Margin) view.marginRight else view.paddingRight
        stockBottom = if (dstBottom == Margin) view.marginBottom else view.paddingBottom
        logInsets()
        applyPadding(insets)
        applyMargin(insets)
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
        if (!isAny(Padding)) return
        view.updatePadding(
            left = if (dstLeft == Padding) stockLeft + insets.left else view.paddingLeft,
            top = if (dstTop == Padding) stockTop + insets.top else view.paddingTop,
            right = if (dstRight == Padding) stockRight + insets.right else view.paddingRight,
            bottom = if (dstBottom == Padding) stockBottom + insets.bottom else view.paddingBottom,
        )
    }

    private fun applyMargin(insets: Insets) {
        if (!isAny(Margin)) return
        view.updateLayoutParams<MarginLayoutParams> {
            leftMargin = if (dstLeft == Margin) stockLeft + insets.left else view.marginLeft
            topMargin = if (dstTop == Margin) stockTop + insets.top else view.marginTop
            rightMargin = if (dstRight == Margin) stockRight + insets.right else view.marginRight
            bottomMargin = if (dstBottom == Margin) stockBottom + insets.bottom else view.marginBottom
        }
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

    private fun logInsets() {
        if (!debugInsets) return
        val left = if (dstLeft.isNone) "" else insets.left.toString()
        val top = if (dstTop.isNone) "" else insets.top.toString()
        val right = if (dstRight.isNone) "" else insets.right.toString()
        val bottom = if (dstBottom.isNone) "" else insets.bottom.toString()
        val types = typeMask.getTypes(windowInsets, !dstLeft.isNone, !dstTop.isNone, !dstRight.isNone, !dstBottom.isNone)
        logd { "${view.nameWithId()} applied[${dstLeft.letter}$left,${dstTop.letter}$top,${dstRight.letter}$right,${dstBottom.letter}$bottom] $types" }
    }
}