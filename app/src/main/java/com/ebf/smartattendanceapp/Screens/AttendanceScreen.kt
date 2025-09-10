// AttendanceScreen.kt

import android.Manifest
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

    // Start/stop ultrasonic listening with screen lifecycle
    DisposableEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            Log.d(TAG, "Permissions granted → onPermissionsGranted()")
            viewModel.onPermissionsGranted()
        }
        onDispose {
            Log.d(TAG, "Screen disposed → stopListening()")
            viewModel.stopListening()
        }
    }

    // Show biometric prompt when AUTHENTICATING
    LaunchedEffect(state, activity) {
        if (state == AttendanceState.AUTHENTICATING) {
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(
                activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Log.d(TAG, "Biometric success")
                        viewModel.onBiometricSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Log.w(TAG, "Biometric error: $errorCode $errString")
                        if (
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            // this function must exist in your ViewModel
                            viewModel.onBiometricFailure(errString.toString())
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Identity Verification")
                .setSubtitle("Verify it's you to mark attendance")
                .setDeviceCredentialAllowed(true) // PIN/Pattern/Password fallback
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            permissionsState.allPermissionsGranted -> {
                // Keep camera mounted; analyzer runs only when SCANNING or SAVING_TO_DB
                CameraView(
                    isScanningActive = (state == AttendanceState.SCANNING || state == AttendanceState.SAVING_TO_DB),
                    onQrScanned = { code ->
                        Log.d(TAG, "onQrScanned -> ${code?.take(200)}")
                        viewModel.onQrScanned(code)
                    },
                    lifecycleOwner = lifecycleOwner
                )

                // Debug label to see the current state
                Text(
                    text = "STATE: $state",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

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
                PermissionRationale(
                    "This feature needs Camera and Mic access for secure attendance. Please grant permissions."
                ) {
                    permissionsState.launchMultiplePermissionRequest()
                }
            }

            else -> {
                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}
