package com.test.rubik

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class RubikRenderer : GLSurfaceView.Renderer {

    lateinit var rubikModel: RubikModel

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val scratch = FloatArray(16)

    // Матрица для накопления вращения всего куба пользователем
    private val accumulatedRotation = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        rubikModel = RubikModel()
        Matrix.setIdentityM(accumulatedRotation, 0)
        
        // Немного повернем куб изначально, чтобы было видно 3 грани
        rotateCube(45f, 45f)
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        rubikModel.animate()

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -12f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Применяем накопленное вращение от свайпов пользователя
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, accumulatedRotation, 0)

        rubikModel.draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
    }

    fun rotateCube(dx: Float, dy: Float) {
        val temp = FloatArray(16)
        val rotX = FloatArray(16)
        val rotY = FloatArray(16)
        
        Matrix.setRotateM(rotX, 0, dy, 1f, 0f, 0f)
        Matrix.setRotateM(rotY, 0, dx, 0f, 1f, 0f)
        
        Matrix.multiplyMM(temp, 0, rotX, 0, accumulatedRotation, 0)
        Matrix.multiplyMM(accumulatedRotation, 0, rotY, 0, temp, 0)
    }
}