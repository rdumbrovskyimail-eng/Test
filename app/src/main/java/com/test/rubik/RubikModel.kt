package com.test.rubik

import android.opengl.GLES20
import android.opengl.Matrix

class RubikModel {
    
    class Cubie(val x: Int, val y: Int, val z: Int) {
        val cube: Cube
        val modelMatrix = FloatArray(16)

        init {
            // Цвета: RGBA
            val black  = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
            val red    = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f) // Front (+z)
            val orange = floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f) // Back (-z)
            val green  = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f) // Left (-x)
            val blue   = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f) // Right (+x)
            val white  = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // Top (+y)
            val yellow = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f) // Bottom (-y)

            // По умолчанию все грани черные (внутренние)
            val faceColors = Array(6) { black }

            // Раскрашиваем только внешние грани
            if (z == 1)  faceColors[0] = red
            if (z == -1) faceColors[1] = orange
            if (x == -1) faceColors[2] = green
            if (x == 1)  faceColors[3] = blue
            if (y == 1)  faceColors[4] = white
            if (y == -1) faceColors[5] = yellow

            cube = Cube(faceColors)

            // Устанавливаем начальную позицию в пространстве
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x.toFloat(), y.toFloat(), z.toFloat())
        }

        fun draw(vpMatrix: FloatArray, program: Int) {
            val mvp = FloatArray(16)
            Matrix.multiplyMM(mvp, 0, vpMatrix, 0, modelMatrix, 0)
            cube.draw(mvp, program)
        }
    }

    private val cubies = mutableListOf<Cubie>()
    private var mProgram: Int = 0

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
        mProgram = ShaderHelper.createProgram(vertexShaderCode, fragmentShaderCode)

        // Генерируем 27 кубиков (3x3x3)
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    cubies.add(Cubie(x, y, z))
                }
            }
        }
    }

    fun draw(vpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)
        for (cubie in cubies) {
            cubie.draw(vpMatrix, mProgram)
        }
    }
}