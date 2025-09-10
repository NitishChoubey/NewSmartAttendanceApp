package com.ebf.smartattendanceapp.Overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ebf.smartattendanceapp.Indicators.FailureIndicator
import com.ebf.smartattendanceapp.Indicators.StatusIndicator
import com.ebf.smartattendanceapp.Indicators.SuccessIndicator
import com.ebf.smartattendanceapp.R
import com.ebf.smartattendanceapp.ViewModel.AttendanceState



@Composable
fun AttendanceOverlay(state: AttendanceState, navController: NavController, onRetry: () -> Unit) {
    val overlayColor = when (state) {
        AttendanceState.SUCCESS -> Color(0xFF006400).copy(alpha = 0.8f) // Dark Green
        AttendanceState.FAILURE -> Color(0xFF8B0000).copy(alpha = 0.8f) // Dark Red
        else -> Color.Black.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(overlayColor).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "Status Animation"
        ) { targetState ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (targetState) {
                    AttendanceState.LISTENING_FOR_AUDIO -> StatusIndicator(iconId = R.drawable.ic_mic, text = "Classroom ko sun rha hai...\nPhone ko sthir rakhein.")
                    AttendanceState.AUTHENTICATING -> StatusIndicator(iconId = R.drawable.ic_fingerprint, text = "Biometric Identity Check\nKripya verify karein.")
                    AttendanceState.SCANNING -> {
                        Box(contentAlignment = Alignment.Center) {
                            ScanningFrame()
                            StatusIndicator(iconId = R.drawable.ic_qr_code_scanner, text = "Verified!\nScreen par QR code scan karein.")
                        }
                    }
                    // ✅ NEW UI STATE
                    AttendanceState.SAVING_TO_DB -> StatusIndicator(iconId = R.drawable.ic_qr_code_scanner, text = "Saving Attendance to Database...")
                    AttendanceState.SUCCESS -> SuccessIndicator(navController)
                    AttendanceState.FAILURE -> FailureIndicator(onRetry)
                    else -> {}
                }
            }
        }
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}