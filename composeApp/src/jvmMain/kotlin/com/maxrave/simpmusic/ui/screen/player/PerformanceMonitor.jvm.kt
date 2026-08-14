package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** Desktop has no battery-saver or thermal APIs; only the frame-timing check applies. */
@Composable
actual fun rememberPlatformFatigueSignal(): State<Boolean> = remember { mutableStateOf(false) }
