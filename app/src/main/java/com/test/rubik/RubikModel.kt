package com.test.rubik

import android.opengl.GLES20
import android.opengl.Matrix

enum class Axis { X, Y, Z }

class RubikModel {
    
    class Cubie(val startX: Int, val startY: Int, val startZ: Int) {
        val cube: Cube
        
        // Логические координаты (обновляются после каждого поворота на 90 градусов)
        var lx = startX
        var ly = startY
        var lz = startZ
        
        // Матрица текущего положения и поворота кубика
        var currentMatrix = FloatArray(16)

        init {
            val black  = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
            val red    = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f) // Front (+z)
            val orange = floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f) // Back (-z)
            val green  = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f) // Left (-x)
            val blue   = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f) // Right (+x)
            val white  = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // Top (+y)
            val yellow = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f) // Bottom (-y)

            val faceColors = Array(6) { black }

            if (startZ == 1)  faceColors[0] = red
            if (startZ == -1) faceColors[1] = orange
            if (startX == -1) faceColors[2] = green
            if (startX == 1)  faceColors[3] = blue
            if (startY == 1)  faceColors[4] = white
            if (startY == -1) faceColors[5] = yellow

            cube = Cube(faceColors)

            Matrix.setIdentityM(currentMatrix, 0)
            Matrix.translateM(currentMatrix, 0, startX.toFloat(), startY.toFloat(), startZ.toFloat())
        }

        // Пересчет логических координат после поворота на 90 градусов
        fun updateLogicalPos(axis: Axis, dir: Int) {
            val tempX = lx; val tempY = ly; val tempZ = lz
            when(axis) {
                Axis.X -> { ly = -dir * tempZ; lz = dir * tempY }
                Axis.Y -> { lx = dir * tempZ; lz = -dir * tempX }
                Axis.Z -> { lx = -dir * tempY; ly = dir * tempX }
            }
        }

        // "Запекание" поворота в матрицу кубика
        fun bakeRotation(axis: Axis, angle: Float) {
            val rot = FloatArray(16)
            Matrix.setIdentityM(rot, 0)
            when(axis) {
                Axis.X -> Matrix.rotateM(rot, 0, angle, 1f, 0f, 0f)
                Axis.Y -> Matrix.rotateM(rot, 0, angle, 0f, 1f, 0f)
                Axis.Z -> Matrix.rotateM(rot, 0, angle, 0f, 0f, 1f)
            }
            val temp = FloatArray(16)
            // Умножаем глобальный поворот на текущую матрицу
            Matrix.multiplyMM(temp, 0, rot, 0, currentMatrix, 0)
            System.arraycopy(temp, 0, currentMatrix, 0, 16)
        }

        fun draw(vpMatrix: FloatArray, program: Int, animMatrix: FloatArray?) {
            val mvp = FloatArray(16)
            if (animMatrix != null) {
                val temp = FloatArray(16)
                Matrix.multiplyMM(temp, 0, animMatrix, 0, currentMatrix, 0)
                Matrix.multiplyMM(mvp, 0, vpMatrix, 0, temp, 0)
            } else {
                Matrix.multiplyMM(mvp, 0, vpMatrix, 0, currentMatrix, 0)
            }
            cube.draw(mvp, program)
        }
    }

    private val cubies = mutableListOf<Cubie>()
    private var mProgram: Int = 0

    // Переменные для анимации
    var isAnimating = false
        private set
    private var animAxis = Axis.X
    private var animSlice = 0
    private var animDir = 1
    private var currentAngle = 0f
    private val targetAngle = 90f
    private val animSpeed = 6f // Скорость поворота

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

        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    cubies.add(Cubie(x, y, z))
                }
            }
        }
    }

    fun startRotation(axis: Axis, slice: Int, dir: Int) {
        if (isAnimating) return
        animAxis = axis
        animSlice = slice
        animDir = dir
        currentAngle = 0f
        isAnimating = true
    }

    fun animate() {
        if (!isAnimating) return
        currentAngle += animSpeed
        if (currentAngle >= targetAngle) {
            val finalAngle = targetAngle * animDir
            for (cubie in cubies) {
                if (isCubieInAnimSlice(cubie)) {
                    cubie.bakeRotation(animAxis, finalAngle)
                    cubie.updateLogicalPos(animAxis, animDir)
                }
            }
            isAnimating = false
        }
    }

    private fun isCubieInAnimSlice(cubie: Cubie): Boolean {
        return when (animAxis) {
            Axis.X -> cubie.lx == animSlice
            Axis.Y -> cubie.ly == animSlice
            Axis.Z -> cubie.lz == animSlice
        }
    }

    fun draw(vpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)
        
        val animRotMatrix = FloatArray(16)
        if (isAnimating) {
            Matrix.setIdentityM(animRotMatrix, 0)
            val angle = currentAngle * animDir
            when (animAxis) {
                Axis.X -> Matrix.rotateM(animRotMatrix, 0, angle, 1f, 0f, 0f)
                Axis.Y -> Matrix.rotateM(animRotMatrix, 0, angle, 0f, 1f, 0f)
                Axis.Z -> Matrix.rotateM(animRotMatrix, 0, angle, 0f, 0f, 1f)
            }
        }

        for (cubie in cubies) {
            if (isAnimating && isCubieInAnimSlice(cubie)) {
                cubie.draw(vpMatrix, mProgram, animRotMatrix)
            } else {
                cubie.draw(vpMatrix, mProgram, null)
            }
        }
    }
}