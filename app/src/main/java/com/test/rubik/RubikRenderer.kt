package com.test.rubik

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class RubikRenderer : GLSurfaceView.Renderer {

    private lateinit var rubikModel: RubikModel

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val scratch = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        rubikModel = RubikModel()
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Шаг анимации вращения слоев
        rubikModel.animate()

        // Если кубик сейчас не анимирует слой, запускаем случайный поворот!
        if (!rubikModel.isAnimating) {
            val randomAxis = Axis.values()[Random.nextInt(3)]
            val randomSlice = Random.nextInt(3) - 1 // -1, 0 или 1
            val randomDir = if (Random.nextBoolean()) 1 else -1
            rubikModel.startRotation(randomAxis, randomSlice, randomDir)
        }

        Matrix.setLookAtM(viewMatrix, 0, 0f, 4f, -12f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Медленно вращаем весь кубик в пространстве, чтобы видеть его со всех сторон
        val time = SystemClock.uptimeMillis() % 12000L
        val angle = 0.030f * time.toInt()
        
        Matrix.setRotateM(rotationMatrix, 0, angle, 1f, 1f, 1f)
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        rubikModel.draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
    }
}