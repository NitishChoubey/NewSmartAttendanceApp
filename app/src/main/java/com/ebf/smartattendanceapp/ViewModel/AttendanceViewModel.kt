package com.ebf.smartattendanceapp.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebf.smartattendanceapp.UltrasonicDetector.UltrasonicDetector
import com.ebf.smartattendanceapp.data.AttendanceRepository
import com.ebf.smartattendanceapp.data.NetResult
import com.ebf.smartattendanceapp.data.net.RetrofitProvider
import com.ebf.smartattendanceapp.session.AppSession
import com.ebf.smartattendanceapp.qr.QrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AttendanceViewModel(
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

    fun onPermissionsGranted() {
        if (_state.value == AttendanceState.REQUESTING_PERMISSIONS) {
            _state.value = AttendanceState.LISTENING_FOR_AUDIO
            startListeningForUltrasonicSound()
        }
    }

    private fun startListeningForUltrasonicSound() {
        stopListening()
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            ultrasonicDetector.startListening()
            ultrasonicDetector.isHearingUltrasonic.collect { isHearing ->
                if (isHearing && isActive) {
                    launch(Dispatchers.Main) {
                        _state.update { current ->
                            if (current == AttendanceState.LISTENING_FOR_AUDIO) AttendanceState.AUTHENTICATING else current
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
    }

    fun onBiometricSuccess() {
        Log.d(TAG, "Biometric success -> SCANNING")
        _state.value = AttendanceState.SCANNING
    }

    fun onBiometricFailure(error: String) {
        Log.w(TAG, "Biometric failure: $error")
        _state.value = AttendanceState.FAILURE
        stopListening()
    }

    fun onQrScanned(qrValue: String?) {
        Log.d(TAG, "onQrScanned() state=${_state.value} qr='${qrValue?.take(200)}'")
        if (_state.value != AttendanceState.SCANNING) {
            Log.w(TAG, "Ignoring QR because not in SCANNING state")
            return
        }

        val rollNo = AppSession.studentId.trim()
        if (rollNo.isBlank()) {
            Log.e(TAG, "rollNo missing in AppSession; cannot post to backend")
            _state.value = AttendanceState.FAILURE
            _state.value = AttendanceState.SCANNING
            return
        }

        // Extract sessionId (JSON or prefix or url) using your QrParser
        val sessionId = QrParser.extractSessionId(qrValue)
        if (sessionId.isNullOrBlank()) {
            Log.w(TAG, "Invalid QR payload, sessionId not found")
            _state.value = AttendanceState.FAILURE
            _state.value = AttendanceState.SCANNING
            return
        }

        // Pause scanning and call backend
        _state.value = AttendanceState.SAVING_TO_DB
        Log.d(TAG, "Posting attendance: roll=$rollNo session=$sessionId")

        viewModelScope.launch {
            when (val r = repo.markAttendance(sessionId)) {
                is NetResult.Ok -> {
                    Log.d(TAG, "Attendance marked successfully")
                    _state.value = AttendanceState.SUCCESS
                }
                is NetResult.Err -> {
                    Log.w(TAG, "markAttendance error: ${r.message}")
                    // show failure briefly then return to scanning
                    _state.value = AttendanceState.FAILURE
                    delay(1500)
                    _state.value = AttendanceState.SCANNING
                }
            }
        }
    }

    fun resetState() {
        _state.value = AttendanceState.REQUESTING_PERMISSIONS
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
