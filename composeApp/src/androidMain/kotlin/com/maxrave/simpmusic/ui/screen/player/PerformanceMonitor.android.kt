package com.maxrave.simpmusic.ui.screen.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Android fatigue signals: power-save mode (battery saver, watched via broadcast) or moderate+
 * thermal throttling (polled — the thermal status broadcast is @SystemApi so not reachable
 * without a signature permission). Polling [PowerManager.currentThermalStatus] every few
 * seconds is negligible.
 */
@Composable
actual fun rememberPlatformFatigueSignal(): State<Boolean> {
    val context = LocalContext.current
    val fatigued = remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        fun update() {
            val powerSave = powerManager.isPowerSaveMode
            val thermal =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
                } else {
                    false
                }
            fatigued.value = powerSave || thermal
        }

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = update()
            }
        update()
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5_000)
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val thermal =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
                } else {
                    false
                }
            fatigued.value = powerManager.isPowerSaveMode || thermal
        }
    }
    return fatigued
}
