package com.test.rubik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                var isLayerMode by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Наш 3D движок
                    AndroidView(
                        factory = { context ->
                            RubikSurfaceView(context)
                        },
                        update = { view ->
                            view.isLayerMode = isLayerMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Кнопка переключения режимов
                    Button(
                        onClick = { isLayerMode = !isLayerMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLayerMode) Color(0xFFE53935) else Color(0xFF1E88E5)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                    ) {
                        Text(
                            text = if (isLayerMode) "Режим: ВРАЩЕНИЕ СЛОЕВ" else "Режим: ВРАЩЕНИЕ КУБА",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}