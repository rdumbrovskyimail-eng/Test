package com.test.rubik

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube(faceColors: Array<FloatArray>) {
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer

    private val vertexCount = 36
    private val vertexStride = 3 * 4
    private val colorStride = 4 * 4

    init {
        // Радиус 0.48f дает размер кубика 0.96f. 
        // При шаге сетки 1.0f это создаст красивые зазоры в 0.04f между кубиками.
        val r = 0.48f
        val vertices = floatArrayOf(
            // Front face (+z)
            -r,  r,  r,  -r, -r,  r,   r, -r,  r,
            -r,  r,  r,   r, -r,  r,   r,  r,  r,
            // Back face (-z)
             r,  r, -r,   r, -r, -r,  -r, -r, -r,
             r,  r, -r,  -r, -r, -r,  -r,  r, -r,
            // Left face (-x)
            -r,  r, -r,  -r, -r, -r,  -r, -r,  r,
            -r,  r, -r,  -r, -r,  r,  -r,  r,  r,
            // Right face (+x)
             r,  r,  r,   r, -r,  r,   r, -r, -r,
             r,  r,  r,   r, -r, -r,   r,  r, -r,
            // Top face (+y)
            -r,  r, -r,  -r,  r,  r,   r,  r,  r,
            -r,  r, -r,   r,  r,  r,   r,  r, -r,
            // Bottom face (-y)
            -r, -r,  r,  -r, -r, -r,   r, -r, -r,
            -r, -r,  r,   r, -r, -r,   r, -r,  r
        )

        val colors = FloatArray(36 * 4)
        // faceColors: 0:Front, 1:Back, 2:Left, 3:Right, 4:Top, 5:Bottom
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

    }

    fun draw(mvpMatrix: FloatArray, program: Int) {
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, colorStride, colorBuffer)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}