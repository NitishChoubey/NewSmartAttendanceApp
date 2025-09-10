package com.ebf.smartattendanceapp.Screens

import android.Manifest
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ebf.smartattendanceapp.CameraView.CameraView
import com.ebf.smartattendanceapp.Overlay.AttendanceOverlay
import com.ebf.smartattendanceapp.Rationale.PermissionRationale
import com.ebf.smartattendanceapp.ViewModel.AttendanceState
import com.ebf.smartattendanceapp.ViewModel.AttendanceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val TAG = "AttendanceScreen"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AttendanceScreen(
    navController: NavController,
    activity: FragmentActivity,
    viewModel: AttendanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state) { Log.d(TAG, "State -> $state") }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    DisposableEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            Log.d(TAG, "Permissions granted -> start listening")
            viewModel.onPermissionsGranted()
        }
        onDispose {
            Log.d(TAG, "Screen disposed -> stop listening")
            viewModel.stopListening()
        }
    }

    LaunchedEffect(state, activity) {
        if (state == AttendanceState.AUTHENTICATING) {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric success")
                    viewModel.onBiometricSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Biometric error: $errorCode $errString")
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        viewModel.onBiometricFailure(errString.toString())
                    } else {
                        navController.popBackStack()
                    }
                }
            })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Identity Verification")
                .setSubtitle("Verify it's you to mark attendance")
                .setDeviceCredentialAllowed(true)
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            permissionsState.allPermissionsGranted -> {
                // IMPORTANT: Keep CameraView mounted (prevents rebinds). Analyzer will only run when state == SCANNING.
                CameraView(
                    isScanningActive = (state == AttendanceState.SCANNING),
                    onQrScanned = { code ->
                        Log.d(TAG, "CameraView.onQrScanned: ${code?.take(120)}")
                        viewModel.onQrScanned(code)
                    },
                    lifecycleOwner = lifecycleOwner
                )

                // Overlay on top (semi-transparent). This does not stop analyzer.
                AttendanceOverlay(
                    state = state,
                    navController = navController,
                    onRetry = {
                        Log.d(TAG, "Retry tapped")
                        navController.popBackStack()
                        navController.navigate("attendance")
                    }
                )
            }

            permissionsState.shouldShowRationale || !permissionsState.allPermissionsGranted -> {
                PermissionRationale("This feature needs Camera and Mic access for secure attendance. Please grant permissions.") {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }

            else -> {
                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) permissionsState.launchMultiplePermissionRequest()
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}
