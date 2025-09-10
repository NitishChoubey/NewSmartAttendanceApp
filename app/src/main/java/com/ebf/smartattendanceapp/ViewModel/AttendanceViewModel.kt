package com.ebf.smartattendanceapp.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebf.smartattendanceapp.UltrasonicDetector.UltrasonicDetector
import com.ebf.smartattendanceapp.data.AttendanceRepository
import com.ebf.smartattendanceapp.data.NetResult
import com.ebf.smartattendanceapp.data.net.RetrofitProvider
import com.ebf.smartattendanceapp.qr.QrParser
import com.ebf.smartattendanceapp.session.AppSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AttendanceViewModel(
    // Default wiring to Retrofit + your saved studentId
    private val repo: AttendanceRepository = AttendanceRepository(
        api = RetrofitProvider.attendanceApi,
        studentIdProvider = { AppSession.studentId }
    )
) : ViewModel() {

    private val TAG = "AttendanceVM"

    private val _state = MutableStateFlow(AttendanceState.REQUESTING_PERMISSIONS)
    val state = _state.asStateFlow()

    private val ultrasonicDetector = UltrasonicDetector()
    private var listeningJob: Job? = null

    // ---------------- Permissions → Ultrasonic ----------------

    fun onPermissionsGranted() {
        if (_state.value == AttendanceState.REQUESTING_PERMISSIONS) {
            Log.d(TAG, "Permissions granted → LISTENING_FOR_AUDIO")
            _state.value = AttendanceState.LISTENING_FOR_AUDIO
            startListeningForUltrasonicSound()
        }
    }

    private fun startListeningForUltrasonicSound() {
        stopListening()
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Ultrasonic listening started")
            ultrasonicDetector.startListening()
            ultrasonicDetector.isHearingUltrasonic.collect { isHearing ->
                if (isHearing && isActive) {
                    Log.d(TAG, "Ultrasonic detected → AUTHENTICATING")
                    launch(Dispatchers.Main) {
                        _state.update { cur ->
                            if (cur == AttendanceState.LISTENING_FOR_AUDIO) AttendanceState.AUTHENTICATING else cur
                        }
                        stopListening()
                    }
                }
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        ultrasonicDetector.stopListening()
        Log.d(TAG, "Ultrasonic listening stopped")
    }

    // ---------------- Biometric ----------------

    fun onBiometricSuccess() {
        Log.d(TAG, "Biometric success → SCANNING")
        _state.value = AttendanceState.SCANNING
    }

    fun onBiometricFailure(error: String) {
        Log.w(TAG, "Biometric failure: $error")
        _state.value = AttendanceState.FAILURE
        stopListening()
    }

    // ---------------- QR Handling ----------------

    fun onQrScanned(qrValue: String?) {
        Log.d(TAG, "onQrScanned() currentState=${_state.value} raw=${qrValue?.take(200)}")

        if (qrValue.isNullOrBlank()) {
            Log.w(TAG, "QR empty → ignoring")
            return
        }

        // Parse sessionId from JSON / prefix / URL
        val sessionId = QrParser.extractSessionId(qrValue)
        if (sessionId.isNullOrBlank()) {
            Log.w(TAG, "No sessionId found in QR → keep scanning")
            return
        }

        val rollNo = AppSession.studentId.trim()
        if (rollNo.isBlank()) {
            Log.e(TAG, "Missing rollNo in AppSession → set it at login")
            return
        }

        // Pause analyzer while we talk to backend
        _state.value = AttendanceState.SAVING_TO_DB
        Log.d(TAG, "POST attendance roll=$rollNo session=$sessionId")

        viewModelScope.launch {
            when (val res = repo.markAttendance(sessionId)) {
                is NetResult.Ok -> {
                    Log.d(TAG, "Attendance marked OK")
                    _state.value = AttendanceState.SUCCESS
                }
                is NetResult.Err -> {
                    Log.w(TAG, "markAttendance error: ${res.message}")
                    _state.value = AttendanceState.FAILURE
                    delay(1500) // brief message, then allow re-scan
                    _state.value = AttendanceState.SCANNING
                }
            }
        }
    }

    fun resetState() {
        Log.d(TAG, "resetState() → REQUESTING_PERMISSIONS")
        _state.value = AttendanceState.REQUESTING_PERMISSIONS
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
