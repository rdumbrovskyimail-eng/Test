import java.io.File
import java.util.Scanner

fun main() {
    val scanner = Scanner(System.`in`)
    val fileName = "notes.txt"
    val file = File(fileName)

    println("--- Простой текстовый редактор ---")
    if (file.exists()) {
        println("Текущее содержимое:")
        println(file.readText())
    }

    println("\nВведите текст (нажмите Enter, чтобы сохранить и выйти):")
    val input = scanner.nextLine()
    
    file.writeText(input)
    println("Сохранено в $fileName")
}
