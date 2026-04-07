package com.example.simplenotes

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.simplenotes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fileName = "notes.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved text
        loadText()

        binding.btnSave.setOnClickListener {
            saveText()
        }

        binding.btnClear.setOnClickListener {
            binding.editText.setText("")
        }

        binding.btnClose.setOnClickListener {
            saveText()
            finish()
        }
    }

    private fun saveText() {
        val text = binding.editText.text.toString()
        try {
            openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(text.toByteArray())
            }
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadText() {
        try {
            openFileInput(fileName).use { input ->
                val text = input.bufferedReader().readText()
                binding.editText.setText(text)
            }
        } catch (e: Exception) {
            // File might not exist yet, ignore
        }
    }
}
