package com.test.app

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Initialize UI
        println("App started")
    }

    fun doSomethingOld() {
        val x = 10
        val y = 20
        println(x + y)
    }
}