package com.test.rubik

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube {
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private var mProgram: Int = 0

    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    private val vertexCount = 36
    private val vertexStride = 3 * 4
    private val colorStride = 4 * 4

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vColor = aColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    init {
        // 6 граней по 2 треугольника = 36 вершин
        val vertices = floatArrayOf(
            // Front face (Red)
            -1f,  1f,  1f,  -1f, -1f,  1f,   1f, -1f,  1f,
            -1f,  1f,  1f,   1f, -1f,  1f,   1f,  1f,  1f,
            // Back face (Orange)
             1f,  1f, -1f,   1f, -1f, -1f,  -1f, -1f, -1f,
             1f,  1f, -1f,  -1f, -1f, -1f,  -1f,  1f, -1f,
            // Left face (Green)
            -1f,  1f, -1f,  -1f, -1f, -1f,  -1f, -1f,  1f,
            -1f,  1f, -1f,  -1f, -1f,  1f,  -1f,  1f,  1f,
            // Right face (Blue)
             1f,  1f,  1f,   1f, -1f,  1f,   1f, -1f, -1f,
             1f,  1f,  1f,   1f, -1f, -1f,   1f,  1f, -1f,
            // Top face (White)
            -1f,  1f, -1f,  -1f,  1f,  1f,   1f,  1f,  1f,
            -1f,  1f, -1f,   1f,  1f,  1f,   1f,  1f, -1f,
            // Bottom face (Yellow)
            -1f, -1f,  1f,  -1f, -1f, -1f,   1f, -1f, -1f,
            -1f, -1f,  1f,   1f, -1f, -1f,   1f, -1f,  1f
        )

        val colors = FloatArray(36 * 4)
        val faceColors = arrayOf(
            floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), // Red
            floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f), // Orange
            floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f), // Green
            floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f), // Blue
            floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), // White
            floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)  // Yellow
        )

        for (i in 0 until 6) {
            for (j in 0 until 6) {
                colors[(i * 6 + j) * 4 + 0] = faceColors[i][0]
                colors[(i * 6 + j) * 4 + 1] = faceColors[i][1]
                colors[(i * 6 + j) * 4 + 2] = faceColors[i][2]
                colors[(i * 6 + j) * 4 + 3] = faceColors[i][3]
            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(colors)
                position(0)
            }
        }

        mProgram = ShaderHelper.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        colorHandle = GLES20.glGetAttribLocation(mProgram, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, colorStride, colorBuffer)

        mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}