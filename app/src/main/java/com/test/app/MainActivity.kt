package com.test.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.test.app.utils.LegacyUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        loadInitialData()
    }

    private fun loadInitialData() {
        val helper = LegacyUtils()
        helper.doSomeHeavyWork()
        println("Data loaded")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        println("Activity destroyed")
    }
}