package com.yagi2.sanacamera.ui.widget

import android.view.MotionEvent


class PinchGestureDetector(private val pinchGestureListener: PinchGestureListener) {

    private var scale = 1.0f

    var distance: Float = 0.toFloat()
        private set

    var preDistance: Float = 0.toFloat()
        private set

    interface PinchGestureListener {
        fun onPinchGestureListener(dragGestureDetector: PinchGestureDetector)
    }

    @Synchronized
    fun onTouchEvent(event: MotionEvent): Boolean {

        val eventX = event.x * scale
        val eventY = event.y * scale
        val count = event.pointerCount

        val action = event.action and MotionEvent.ACTION_MASK
        val actionPointerIndex = event.action and MotionEvent.ACTION_POINTER_INDEX_MASK

        when (action) {
            MotionEvent.ACTION_MOVE -> {

                if (count == 2) {

                    val multiTouchX = event.getX(1) * scale
                    val multiTouchY = event.getY(1) * scale

                    distance = culcDistance(eventX, eventY, multiTouchX, multiTouchY)
                    pinchGestureListener.onPinchGestureListener(this)
                    scale *= distance / preDistance
                    preDistance = distance

                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (count == 2) {
                    val downId = actionPointerIndex shr MotionEvent.ACTION_POINTER_INDEX_SHIFT

                    val multiTouchX = event.getX(downId) * scale
                    val multiTouchY = event.getY(downId) * scale

                    distance = culcDistance(eventX, eventY, multiTouchX, multiTouchY)
                    pinchGestureListener.onPinchGestureListener(this)
                    preDistance = distance
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {

                distance = 0f
                preDistance = 0f
                scale = 1.0f
            }
        }
        return false
    }

    private fun culcDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}