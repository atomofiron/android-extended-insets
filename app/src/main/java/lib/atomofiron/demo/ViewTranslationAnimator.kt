package lib.atomofiron.demo

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
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

    fun hide() {
        reset()
        val translation = view.translationY
        val target = when (gravity) {
            Gravity.Top -> -view.height
            Gravity.Bottom -> view.height
        }.toFloat()
        anim = ValueAnimator.ofFloat(translation, target).apply {
            duration = (300f / view.height * (view.height - abs(translation))).toLong()
            interpolator = AccelerateInterpolator()
            addUpdateListener(this@ViewTranslationAnimator)
            addListener(onEnd = { reset() })
            start()
        }
    }

    fun show() {
        reset()
        val translation = view.translationY
        anim = ValueAnimator.ofFloat(translation, 0f).apply {
            duration = (500f / view.height * abs(translation)).toLong()
            interpolator = DecelerateInterpolator(4f)
            addUpdateListener(this@ViewTranslationAnimator)
            addListener(onEnd = { reset() })
            start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
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