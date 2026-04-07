package com.example.simplenotes

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private val fileName = "notes.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnClose: Button = findViewById(R.id.btnClose)

        // Load saved text
        loadText()

        btnSave.setOnClickListener {
            saveText()
        }

        btnClose.setOnClickListener {
            saveText()
            finish()
        }
    }

    private fun saveText() {
        val text = editText.text.toString()
        try {
            openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(text.toByteArray())
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadText() {
        try {
            openFileInput(fileName).use { input ->
                val text = input.bufferedReader().readText()
                editText.setText(text)
            }
        } catch (e: Exception) {
            // File might not exist yet
        }
    }
}
