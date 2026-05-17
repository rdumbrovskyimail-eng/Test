package com.test.app

import android.app.Application

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        initializeDependencies()
        setupCrashHandler()
    }

    private fun initializeDependencies() {
        // Init logic
        println("Dependencies initialized")
    }

    private fun setupCrashHandler() {
        println("Crashlytics initialized")
        println("Crash handler is ready")
    }
}