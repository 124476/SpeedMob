package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.sqrt
import kotlin.math.absoluteValue
class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val threshold = 0.5
    private val offset = 35.31
    private val accelerations = mutableListOf<Double>()
    private var adjustedSpeed by mutableStateOf(0.0)
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            calculateAndUpdateSpeed()
            handler.postDelayed(this, 500)
        }
    }
    private var kalmanFilter = KalmanFilter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
        handler.postDelayed(updateRunnable, 500)
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)) {
                        SpeedDisplay(
                            speed = adjustedSpeed,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Text(
                            text = "made ravilov/r/r",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(updateRunnable)
    }
    override fun onSensorChanged(event: SensorEvent) {
        val newAcceleration = sqrt(
            (event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]).toDouble()
        )
        if (newAcceleration > threshold) {
            val filteredAcceleration = kalmanFilter.update(newAcceleration * 3.6 - offset)
            accelerations.add(filteredAcceleration)
        } else {
            accelerations.add(0.0)
        }
    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    private fun calculateAndUpdateSpeed() {
        if (accelerations.isNotEmpty()) {
            val averageAcceleration = accelerations.average().absoluteValue
            adjustedSpeed = averageAcceleration
            accelerations.clear()
        }
    }
}
class KalmanFilter {
    private var q = 0.00001
    private var r = 0.01
    private var x = 0.0
    private var p = 1.0
    private var k = 0.0
    fun update(measurement: Double): Double {
        p += q
        k = p / (p + r)
        x += k * (measurement - x)
        p *= (1 - k)
        return x
    }
}
@Composable
fun SpeedDisplay(speed: Double, modifier: Modifier = Modifier) {
    Text(
        text = "%.2f km/h".format(speed),
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold
    )
}
@Preview(showBackground = true)
@Composable
fun SpeedDisplayPreview() {
    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SpeedDisplay(speed = 0.0, modifier = Modifier)
            Text(
                text = "made ravilov/r/r",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}