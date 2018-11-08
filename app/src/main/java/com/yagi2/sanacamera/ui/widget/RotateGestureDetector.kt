package com.yagi2.sanacamera.ui.widget

import android.view.MotionEvent

class RotateGestureDetector(private val rotationGestureListener: RotateGestureListener) {

    var deltaAngle: Float = 0.toFloat()
        private set

    private var downX = 0f
    private var downY = 0f

    private var downX2 = 0f
    private var downY2 = 0f

    private var isFirstPointerUp = false

    interface RotateGestureListener {
        fun onRotation(detector: RotateGestureDetector)
    }

    @Synchronized
    fun onTouchEvent(event: MotionEvent): Boolean {

        val eventX = event.x
        val eventY = event.y
        val count = event.pointerCount

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                downX = eventX
                downY = eventY
                if (count >= 2) {
                    downX2 = event.getX(1)
                    downY2 = event.getY(1)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                downX2 = event.getX(1)
                downY2 = event.getY(1)
            }
            MotionEvent.ACTION_MOVE ->
                if (count >= 2) {
                    val angle = getAngle(downX, downY, downX2, downY2, eventX, eventY, event.getX(1), event.getY(1))

                    if (angle != SLOPE_0.toFloat()) {
                        this.deltaAngle -= (angle * 180.0 / Math.PI).toFloat()
                    }

                    downX2 = event.getX(1)
                    downY2 = event.getY(1)

                    rotationGestureListener.onRotation(this)
                }
            MotionEvent.ACTION_POINTER_UP -> when (event.action) {
                MotionEvent.ACTION_POINTER_INDEX_MASK -> isFirstPointerUp = true
            }
        }

        if (isFirstPointerUp) {
            downX = downX2
            downY = downY2
            isFirstPointerUp = false
        } else {
            downX = eventX
            downY = eventY
        }

        return true
    }

    companion object {

        const val SLOPE_0 = 10000

        private fun getAngle(
            xi1: Float,
            yi1: Float,
            xm1: Float,
            ym1: Float,
            xi2: Float,
            yi2: Float,
            xm2: Float,
            ym2: Float
        ): Float {
            val firstLinearSlope: Float
            if (xm1 - xi1 != 0f && ym1 - yi1 != 0f) {
                firstLinearSlope = (xm1 - xi1) / (ym1 - yi1)
            } else {
                return SLOPE_0.toFloat()
            }

            var secondLinearSlope: Float
            if (xm2 - xi2 != 0f && ym2 - yi2 != 0f) {
                secondLinearSlope = (xm2 - xi2) / (ym2 - yi2)
            } else {
                return SLOPE_0.toFloat()
            }

            if (firstLinearSlope * secondLinearSlope == -1f) {
                return 90.0f
            }

            val tan = (secondLinearSlope - firstLinearSlope) / (1 + secondLinearSlope * firstLinearSlope)

            return Math.atan(tan.toDouble()).toFloat()
        }
    }
}