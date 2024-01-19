package demo.atomofiron.insets

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import kotlin.math.abs

enum class Gravity {
    Top, Bottom
}

class ViewTranslationAnimator(
    private val view: View,
    private val gravity: Gravity,
    private val callback: () -> Unit,
) : ValueAnimator.AnimatorUpdateListener {

    private var anim: ValueAnimator? = null

    private val maxOffset get() = when (gravity) {
        Gravity.Top -> -view.bottom
        Gravity.Bottom -> (view.parent as View).height - view.top
    }.toFloat()

    fun hide() {
        reset()
        val translation = view.translationY
        anim = ValueAnimator.ofFloat(translation, maxOffset).apply {
            duration = (300f / abs(maxOffset) * (abs(maxOffset - translation))).toLong()
            interpolator = AccelerateInterpolator()
            addUpdateListener(this@ViewTranslationAnimator)
            addListener(onEnd = {
                view.isInvisible = true
                reset()
            })
            start()
        }
    }

    fun show() {
        reset()
        val translation = view.translationY
        anim = ValueAnimator.ofFloat(translation, 0f).apply {
            duration = abs((500f / maxOffset * translation)).toLong()
            interpolator = DecelerateInterpolator(4f)
            addUpdateListener(this@ViewTranslationAnimator)
            addListener(onEnd = { reset() })
            start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (view.isInvisible) view.isVisible = true
        view.translationY = animation.animatedValue as Float
        callback()
    }

    private fun reset() {
        anim?.cancel()
        anim?.removeAllListeners()
        anim?.removeUpdateListener(this)
        anim = null
    }
}