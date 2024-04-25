package lib.atomofiron.insets

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.isGone


private var nextKey = 0

internal class ViewInsetsSourceImpl(
    override val view: View,
    val horizontal: Boolean,
    val vertical: Boolean,
    val callback: ViewInsetsSourceCallback,
) : ViewInsetsSource,
    ViewDelegate,
    View.OnAttachStateChangeListener,
    View.OnLayoutChangeListener,
    ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnPreDrawListener {

    override val nameWithId = view.nameWithId()

    private var viewPlaced = !view.isGone
    private var tX = view.translationX
    private var tY = view.translationY
    private var provider: InsetsProvider? = null
    private val isAttached get() = provider != null
    private val key = nextKey++

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
        view.addOnLayoutChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        provider = view.findInsetsProvider()
        logd { "$nameWithId was attached, provider? ${provider != null}" }
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
        view.viewTreeObserver.addOnPreDrawListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        logd { "$nameWithId was detached, provider? ${provider != null}" }
        provider?.revokeInsetsFrom(key)
        provider = null
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        view.viewTreeObserver.removeOnPreDrawListener(this)
    }

    override fun onGlobalLayout() {
        val placed = !view.isGone
        if (placed != viewPlaced) {
            viewPlaced = placed
            if (horizontal || vertical) {
                logd { "$nameWithId become placed? $placed, invalidate insets, provider? ${provider != null}" }
                invalidateInsets()
            }
        }
    }

    override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldRight: Int, oldTop: Int, oldBottom: Int) {
        val horizontal = left != oldLeft || right != oldRight
        val vertical = top != oldTop || bottom != oldBottom
        if (this.horizontal && horizontal || this.vertical && vertical) {
            logd { "$nameWithId layout was changed, invalidate insets, provider? ${provider != null}" }
            invalidateInsets()
        }
    }

    override fun onPreDraw(): Boolean {
        if (horizontal && tX != view.translationX || vertical && tY != view.translationY) {
            tX = view.translationX
            tY = view.translationY
            logd { "$nameWithId translation was changed, invalidate insets, provider? ${provider != null}" }
            invalidateInsets()
        }
        return true
    }

    override fun invalidateInsets() {
        if (!isAttached) return
        var source = InsetsSource.callback(view)
        debug {
            source = source.copy(debugData = view.nameWithId())
        }
        provider?.submitInsets(key, source)
    }
}