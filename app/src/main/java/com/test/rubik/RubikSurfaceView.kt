package com.test.rubik

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.abs

class RubikSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: RubikRenderer
    var isLayerMode = false

    private var previousX = 0f
    private var previousY = 0f
    private var startX = 0f
    private var startY = 0f

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        renderer = RubikRenderer()
        setRenderer(renderer)

        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = x
                previousY = y
                startX = x
                startY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isLayerMode) {
                    val dx = x - previousX
                    val dy = y - previousY
                    renderer.rotateCube(dx * 0.5f, dy * 0.5f)
                    previousX = x
                    previousY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isLayerMode) {
                    val dx = x - startX
                    val dy = y - startY
                    if (abs(dx) > 100 || abs(dy) > 100) {
                        handleLayerSwipe(startX, startY, dx, dy)
                    }
                }
            }
        }
        return true
    }

    private fun handleLayerSwipe(startX: Float, startY: Float, dx: Float, dy: Float) {
        if (renderer.rubikModel.isAnimating) return

        val w = width.toFloat()
        val h = height.toFloat()

        if (abs(dx) > abs(dy)) {
            val slice = if (startY < h / 3) 1 else if (startY < 2 * h / 3) 0 else -1
            val dir = if (dx > 0) 1 else -1
            renderer.rubikModel.startRotation(Axis.Y, slice, dir)
        } else {
            val slice = if (startX < w / 3) -1 else if (startX < 2 * w / 3) 0 else 1
            val dir = if (dy > 0) 1 else -1
            renderer.rubikModel.startRotation(Axis.X, slice, dir)
        }
    }
}