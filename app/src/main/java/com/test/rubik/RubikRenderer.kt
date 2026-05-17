package com.test.rubik

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RubikRenderer : GLSurfaceView.Renderer {

    private lateinit var cube: Cube

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val scratch = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Темно-серый фон
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        // Включаем тест глубины для правильного отображения 3D
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        cube = Cube()
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Камера: позиция (0, 3, -10), смотрит в центр (0,0,0)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 3f, -10f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Вращение куба для демонстрации
        val time = SystemClock.uptimeMillis() % 4000L
        val angle = 0.090f * time.toInt()
        
        Matrix.setRotateM(rotationMatrix, 0, angle, 1f, 1f, 0f)
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        cube.draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        // Настройка перспективы
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
    }
}