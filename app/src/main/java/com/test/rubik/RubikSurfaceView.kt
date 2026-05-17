package com.test.rubik

import android.content.Context
import android.opengl.GLSurfaceView

class RubikSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: RubikRenderer

    init {
        // Используем OpenGL ES 2.0
        setEGLContextClientVersion(2)
        
        // Включаем буфер глубины (минимум 16 бит)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        renderer = RubikRenderer()
        setRenderer(renderer)

        // Рендерим постоянно (для анимации)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}