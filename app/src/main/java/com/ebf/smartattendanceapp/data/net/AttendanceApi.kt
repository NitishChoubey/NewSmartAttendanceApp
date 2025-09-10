package com.ebf.smartattendanceapp.data.net

import retrofit2.http.Body
import retrofit2.http.POST

// --- Request/Response DTOs ---
data class MarkAttendanceRequest(
    val rollNo: String,
    val sessionId: String
)

data class MarkAttendanceResponse(
    val success: Boolean,
    val message: String? = null
)

// --- Retrofit API ---
interface AttendanceApi {
    @POST("api/attendance/mark")
    suspend fun markAttendance(
        @Body body: MarkAttendanceRequest
    ): MarkAttendanceResponse
}
