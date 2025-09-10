package com.ebf.smartattendanceapp.data

import com.ebf.smartattendanceapp.data.net.AttendanceApi
import com.ebf.smartattendanceapp.data.net.MarkAttendanceRequest

// Simple result wrapper
sealed class NetResult<out T> {
    data class Ok<T>(val data: T): NetResult<T>()
    data class Err(val message: String): NetResult<Nothing>()
}

class AttendanceRepository(
    private val api: AttendanceApi,
    private val studentIdProvider: () -> String // returns rollNo
) {
    suspend fun markAttendance(sessionId: String): NetResult<Unit> {
        return try {
            val roll = studentIdProvider().trim()
            val body = MarkAttendanceRequest(rollNo = roll, sessionId = sessionId)
            val resp = api.markAttendance(body)
            if (resp.success) NetResult.Ok(Unit)
            else NetResult.Err(resp.message ?: "Failed")
        } catch (t: Throwable) {
            NetResult.Err(t.message ?: "Network error")
        }
    }
}

