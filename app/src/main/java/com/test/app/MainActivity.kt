package com.test.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        loadInitialData()
    }

    private fun loadInitialData() {
        println("Loading data asynchronously...")
        println("Data loaded")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        println("Activity destroyed")
    }
}