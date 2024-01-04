package lib.atomofiron.demo

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

private const val SIZE = 68f

class CutoutDrawable : Drawable() {

    private enum class Gravity {
        Left, Top, Right
    }

    private val paint = Paint()
    private val path = Path()
    private val haloColor = Color.parseColor("#111111")
    private val lensColor = Color.parseColor("#111122")
    private val glareColor = Color.parseColor("#1a1a22")

    private var gravity = Gravity.Top

    init {
        paint.strokeWidth = 2f
        paint.color = Color.BLACK
        path.moveTo(0f, SIZE)
        path.rCubicTo(54f, 0f, 44f, -68f, 128f, -SIZE)
        path.rLineTo(-256f, 0f)
        path.rCubicTo(84f, 0f, 74f, SIZE, 128f, SIZE)
    }

    fun left() {
        gravity = Gravity.Left
    }

    fun top() {
        gravity = Gravity.Top
    }

    fun right() {
        gravity = Gravity.Right
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun draw(canvas: Canvas) {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        when (gravity) {
            Gravity.Left -> {
                canvas.rotate(-90f)
                canvas.translate(-centerY, 0f)
            }
            Gravity.Top -> canvas.translate(centerX, 0f)
            Gravity.Right -> {
                canvas.rotate(90f)
                canvas.translate(centerY, -bounds.width().toFloat())
            }
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawPath(path, paint)
        canvas.translate(0f, SIZE / 2)
        paint.color = haloColor
        canvas.drawCircle(0f, 0f, 24f, paint)
        paint.color = lensColor
        canvas.drawCircle(0f, 0f, 16f, paint)
        paint.color = glareColor
        canvas.drawCircle(-8f, -8f, 4f, paint)
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(0f, 0f, 16f, paint)
    }
}
