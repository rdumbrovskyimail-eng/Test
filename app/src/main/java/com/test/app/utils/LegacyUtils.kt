package com.test.app.utils

class LegacyUtils {

    fun doSomeHeavyWork() {
        Thread.sleep(1000)
        println("Heavy work done on main thread! This is very bad!")
    }

    fun parseJsonManually(json: String): Map<String, String> {
        // very old implementation
        return emptyMap()
    }
}