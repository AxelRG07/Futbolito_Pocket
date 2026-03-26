package com.example.futbolito_pocket

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futbolito_pocket.ui.theme.Futbolito_PocketTheme
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Futbolito_PocketTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaPruebaSensores()
                }
            }
        }
    }
}

@Composable
fun PantallaPruebaSensores() {

    var valorX by remember { mutableStateOf(0f) }
    var valorY by remember { mutableStateOf(0f) }

    EscuchadorGravedad { x, y ->
        valorX = x
        valorY = y
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sensor de Gravedad",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Eje X (Lateral): ${String.format("%.2f", valorX)}",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Eje Y (Vertical): ${String.format("%.2f", valorY)}",
            fontSize = 24.sp
        )
    }
}

@Composable
fun EscuchadorGravedad(
    onGravedadCambiada: (x: Float, y: Float) -> Unit
) {
    val contexto = LocalContext.current

    val sensorManager = remember {
        contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val sensorGravedad = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    DisposableEffect(Unit) {

        val listener = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent?) {
                if (evento?.sensor?.type == Sensor.TYPE_GRAVITY) {
                    val x = evento.values[0]
                    val y = evento.values[1]
                    onGravedadCambiada(x, y)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        sensorGravedad?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}