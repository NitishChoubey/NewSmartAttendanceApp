package com.ebf.smartattendanceapp.data.net

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

// --- Request/Response DTOs ---
data class MarkAttendanceRequest(
    @SerializedName("rollNo")
    val rollNo: String,
    @SerializedName("sessionId")
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
