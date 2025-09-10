// AttendanceRepository.kt
package com.ebf.smartattendanceapp.data

import android.util.Log
import com.ebf.smartattendanceapp.data.net.AttendanceApi
import com.ebf.smartattendanceapp.data.net.MarkAttendanceRequest

sealed class NetResult<out T> {
    data class Ok<T>(val data: T): NetResult<T>()
    data class Err(val message: String): NetResult<Nothing>()
}

class AttendanceRepository(
    private val api: AttendanceApi,
    private val studentIdProvider: () -> String
) {
    private val TAG = "AttendanceRepo"

    suspend fun markAttendance(sessionId: String): NetResult<Unit> {
        val roll = studentIdProvider().trim()
        return try {
            Log.d(TAG, "POST /api/attendance/mark roll=$roll session=$sessionId")
            val resp = api.markAttendance(MarkAttendanceRequest(rollNo = roll, sessionId = sessionId))
            if (resp.success) {
                Log.d(TAG, "Backend OK: ${resp.message}")
                NetResult.Ok(Unit)
            } else {
                Log.w(TAG, "Backend FAIL: ${resp.message}")
                NetResult.Err(resp.message ?: "Failed")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Network/Parsing error: ${t.message}", t)
            NetResult.Err(t.message ?: "Network error")
        }
    }
}
