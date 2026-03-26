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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
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
                    CanchaFutbolito()
                }
            }
        }
    }
}

data class Pared(
    val inicio: Offset,
    val fin: Offset,
    val esHorizontal: Boolean
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CanchaFutbolito() {
    var gravedadX by remember { mutableStateOf(0f) }
    var gravedadY by remember { mutableStateOf(0f) }
    EscuchadorGravedad { x, y -> gravedadX = x; gravedadY = y }

    var posX by remember { mutableStateOf(0f) }
    var posY by remember { mutableStateOf(0f) }
    var velX by remember { mutableStateOf(0f) }
    var velY by remember { mutableStateOf(0f) }
    var marcadorVisitante by remember { mutableIntStateOf(0) }
    var marcadorLocal by remember { mutableIntStateOf(0) }

    val radioPelota = 25f
    val rebote = -0.5f
    val friccion = 0.995f
    val multiplicadorFuerza = 400f

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val densidad = LocalDensity.current
            val anchoMaximo = with(densidad) { maxWidth.toPx() }
            val altoMaximo = with(densidad) { maxHeight.toPx() }

            val anchoPorteria = anchoMaximo * 0.3f
            val inicioPorteriaX = (anchoMaximo - anchoPorteria) / 2f
            val finPorteriaX = inicioPorteriaX + anchoPorteria

            val listaParedes = remember(anchoMaximo, altoMaximo) {
                val w = anchoMaximo
                val h = altoMaximo

                listOf(
                    Pared(Offset(w * 0.15f, h * 0.15f), Offset(w * 0.35f, h * 0.15f), true), // Arriba Izq
                    Pared(Offset(w * 0.65f, h * 0.15f), Offset(w * 0.85f, h * 0.15f), true), // Arriba Der
                    Pared(Offset(w * 0.15f, h * 0.85f), Offset(w * 0.35f, h * 0.85f), true), // Abajo Izq
                    Pared(Offset(w * 0.65f, h * 0.85f), Offset(w * 0.85f, h * 0.85f), true), // Abajo Der

                    Pared(Offset(w * 0.0f, h * 0.3f), Offset(w * 0.2f, h * 0.3f), true),
                    Pared(Offset(w * 0.8f, h * 0.3f), Offset(w * 1.0f, h * 0.3f), true),
                    Pared(Offset(w * 0.0f, h * 0.7f), Offset(w * 0.2f, h * 0.7f), true),
                    Pared(Offset(w * 0.8f, h * 0.7f), Offset(w * 1.0f, h * 0.7f), true),

                    Pared(Offset(w * 0.4f, h * 0.5f), Offset(w * 0.6f, h * 0.5f), true),     // Centro horizontal
                    Pared(Offset(w * 0.25f, h * 0.4f), Offset(w * 0.25f, h * 0.6f), false),  // Medio Izq vertical
                    Pared(Offset(w * 0.75f, h * 0.4f), Offset(w * 0.75f, h * 0.6f), false),  // Medio Der vertical

                    Pared(Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.35f), false),  // Top mid
                    Pared(Offset(w * 0.5f, h * 0.65f), Offset(w * 0.5f, h * 0.75f), false)   // Bot mid
                )
            }

            fun reiniciarPelota() {
                posX = anchoMaximo / 2f
                posY = altoMaximo / 2f
                velX = 0f
                velY = 0f
            }
            LaunchedEffect(Unit) { reiniciarPelota() }

            LaunchedEffect(gravedadX, gravedadY) {
                var ultimoTiempo = withFrameNanos { it }

                while (isActive) {
                    withFrameNanos { tiempoActual ->
                        val dt = (tiempoActual - ultimoTiempo) / 1_000_000_000f
                        ultimoTiempo = tiempoActual

                        velX += (-gravedadX * multiplicadorFuerza) * dt
                        velY += (gravedadY * multiplicadorFuerza) * dt
                        velX *= friccion
                        velY *= friccion
                        posX += velX * dt
                        posY += velY * dt


                        if (posX < radioPelota) { posX = radioPelota; velX *= rebote }
                        else if (posX > anchoMaximo - radioPelota) { posX = anchoMaximo - radioPelota; velX *= rebote }
                        if (posY < radioPelota) {
                            if (posX in inicioPorteriaX..finPorteriaX) { marcadorLocal++; reiniciarPelota() }
                            else { posY = radioPelota; velY *= rebote }
                        } else if (posY > altoMaximo - radioPelota) {
                            if (posX in inicioPorteriaX..finPorteriaX) { marcadorVisitante++; reiniciarPelota() }
                            else { posY = altoMaximo - radioPelota; velY *= rebote }
                        }

                        listaParedes.forEach { pared ->
                            if (pared.esHorizontal) {
                                val xMin = minOf(pared.inicio.x, pared.fin.x) - radioPelota
                                val xMax = maxOf(pared.inicio.x, pared.fin.x) + radioPelota

                                if (posX in xMin..xMax) {
                                    val paredY = pared.inicio.y

                                    if (posY + radioPelota >= paredY && velY > 0 && posY < paredY) {
                                        posY = paredY - radioPelota
                                        velY *= rebote
                                    }
                                    else if (posY - radioPelota <= paredY && velY < 0 && posY > paredY) {
                                        posY = paredY + radioPelota
                                        velY *= rebote
                                    }
                                }
                            } else {

                                val yMin = minOf(pared.inicio.y, pared.fin.y) - radioPelota
                                val yMax = maxOf(pared.inicio.y, pared.fin.y) + radioPelota

                                if (posY in yMin..yMax) {
                                    val paredX = pared.inicio.x

                                    if (posX + radioPelota >= paredX && velX > 0 && posX < paredX) {
                                        posX = paredX - radioPelota
                                        velX *= rebote
                                    }
                                    else if (posX - radioPelota <= paredX && velX < 0 && posX > paredX) {
                                        posX = paredX + radioPelota
                                        velX *= rebote
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFF4CAF50))

                val profundidadPorteria = 50f

                drawRect(
                    color = Color.Red.copy(alpha = 0.5f),
                    topLeft = Offset(inicioPorteriaX, 0f),
                    size = Size(anchoPorteria, profundidadPorteria)
                )

                drawRect(
                    color = Color.Blue.copy(alpha = 0.5f),
                    topLeft = Offset(inicioPorteriaX, altoMaximo - profundidadPorteria),
                    size = Size(anchoPorteria, profundidadPorteria)
                )

                listaParedes.forEach { pared ->
                    drawLine(
                        color = Color.White,
                        start = pared.inicio,
                        end = pared.fin,
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.3f),
                        start = pared.inicio,
                        end = pared.fin,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(color = Color.White, radius = radioPelota, center = Offset(posX, posY))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Visita: $marcadorVisitante", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Local: $marcadorLocal", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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