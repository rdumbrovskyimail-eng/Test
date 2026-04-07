package com.example.notes

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editText = EditText(this).apply {
            hint = "Введите текст..."
        }
        val button = Button(this).apply {
            text = "Сохранить"
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(editText)
            addView(button)
        }
        setContentView(layout)

        val prefs = getSharedPreferences("notes", Context.MODE_PRIVATE)

        // Загрузка
        editText.setText(prefs.getString("saved_text", ""))

        // Сохранение
        button.setOnClickListener {
            prefs.edit().putString("saved_text", editText.text.toString()).apply()
        }
    }
}
