package com.test.app

fun calculateSum(a: Int, b: Int): Int {
    return a + b
}

@Deprecated("Do not use this anymore")
fun oldLegacyMethod() {
    println("This should be deleted")
}

fun printMessage(msg: String) {
    println("MSG: $msg")
}