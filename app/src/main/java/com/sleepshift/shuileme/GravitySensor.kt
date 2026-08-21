package com.sleepshift.shuileme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SL-9.1：加速度计重力传感器。
 * - 无危险权限（加速度计不需要权限）；
 * - 无传感器设备返回 null → UI 降级为模拟重力。
 * 输出归一化倾斜方向（-1..1）。
 */
class GravitySensor(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val _tilt = MutableStateFlow<Pair<Float, Float>?>(null)
    val tilt: StateFlow<Pair<Float, Float>?> = _tilt

    val hasSensor: Boolean get() = sensor != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val ax = event.values[0] / SensorManager.GRAVITY_EARTH
            val ay = event.values[1] / SensorManager.GRAVITY_EARTH
            _tilt.value = ax to ay
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        _tilt.value = null
    }
}
