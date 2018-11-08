package com.yagi2.sanacamera.ui.widget

import android.content.Context
import android.graphics.PointF
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.yagi2.sanacamera.ui.widget.PinchGestureDetector.PinchGestureListener
import com.yagi2.sanacamera.ui.widget.RotateGestureDetector.RotateGestureListener



class CanMoveImageView : AppCompatImageView, View.OnTouchListener {

    companion object {
        const val GESTURE_DRAGGABLE = 0x0001
        const val GESTURE_ROTATABLE = 0x0002
        const val GESTURE_SCALABLE = 0x0004

        const val DEFAULT_LIMIT_SCALE_MAX = 5.0f
        const val DEFAULT_LIMIT_SCALE_MIN = 0.3f
    }

    private var limitScaleMax = DEFAULT_LIMIT_SCALE_MAX
    private var limitScaleMin = DEFAULT_LIMIT_SCALE_MIN

    private var scaleFactor = 1.0f

    private var dragGestureDetector: DragGestureDetector? = null
    private var pinchGestureDetector: PinchGestureDetector? = null
    private var rotateGestureDetector: RotateGestureDetector? = null

    private var angle: Float = 0.toFloat()

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(GESTURE_DRAGGABLE or GESTURE_ROTATABLE or GESTURE_SCALABLE)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(GESTURE_DRAGGABLE or GESTURE_ROTATABLE or GESTURE_SCALABLE)
    }

    constructor(context: Context) : super(context) {
        init(GESTURE_DRAGGABLE or GESTURE_ROTATABLE or GESTURE_SCALABLE)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (dragGestureDetector != null) {
            dragGestureDetector?.onTouchEvent(event)
        }

        if (rotateGestureDetector != null) {
            rotateGestureDetector?.onTouchEvent(event)
        }

        if (pinchGestureDetector != null) {
            pinchGestureDetector?.onTouchEvent(event)
        }

        return true
    }

    private fun init(gestureFlag: Int) {
        setOnTouchListener(this)

        if (gestureFlag and GESTURE_DRAGGABLE == GESTURE_DRAGGABLE) {
            dragGestureDetector = DragGestureDetector(DragListener())
        }

        if (gestureFlag and GESTURE_ROTATABLE == GESTURE_ROTATABLE) {
            rotateGestureDetector = RotateGestureDetector(RotateListener())
        }

        if (gestureFlag and GESTURE_SCALABLE == GESTURE_SCALABLE) {
            pinchGestureDetector = PinchGestureDetector(ScaleListener())
        }
    }

    private fun rotateXY(centerX: Float, centerY: Float, angle: Float, x: Float, y: Float): PointF {
        val rad = Math.toRadians(angle.toDouble())

        val resultX = ((x - centerX) * Math.cos(rad) - (y - centerY) * Math.sin(rad) + centerX).toFloat()
        val resultY = ((x - centerX) * Math.sin(rad) + (y - centerY) * Math.cos(rad) + centerY.toDouble()).toFloat()

        return PointF(resultX, resultY)
    }

    private inner class DragListener : DragGestureDetector.DragGestureListener {
        @Synchronized
        override fun onDragGestureListener(dragGestureDetector: DragGestureDetector) {

            var dx = dragGestureDetector.deltaX
            var dy = dragGestureDetector.deltaY
            val pf = rotateXY(0f, 0f, angle, dx, dy)

            dx = pf.x
            dy = pf.y

            x += dx * scaleFactor
            y += dy * scaleFactor
        }
    }

    private inner class RotateListener : RotateGestureListener {
        override fun onRotation(detector: RotateGestureDetector) {
            angle += detector.deltaAngle.toInt()
            rotation += detector.deltaAngle
        }
    }

    private inner class ScaleListener : PinchGestureListener {
        override fun onPinchGestureListener(dragGestureDetector: PinchGestureDetector) {

            val scale = dragGestureDetector.distance / dragGestureDetector.preDistance
            val tmpScale = scaleFactor * scale

            if (tmpScale in limitScaleMin..limitScaleMax) {
                scaleFactor = tmpScale
                scaleX = scaleFactor
                scaleY = scaleFactor

                return
            }
        }
    }
}