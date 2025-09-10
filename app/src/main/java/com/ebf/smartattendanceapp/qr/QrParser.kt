package com.ebf.smartattendanceapp.qr

import android.net.Uri
import org.json.JSONObject

object QrParser {
    fun extractSessionId(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        // 1) JSON payload (your dashboard format)
        if (raw.trim().startsWith("{")) {
            runCatching {
                val obj = JSONObject(raw)
                val sid = obj.optString("sessionId")
                if (sid.isNotBlank()) return sid
            }
        }

        // 2) Simple prefix (fallback)
        if (raw.startsWith("CLASS_SESSION_")) {
            return raw.substringAfter("CLASS_SESSION_").takeIf { it.isNotBlank() }
        }

        // 3) URL with sid / sessionId as query param (fallback)
        runCatching {
            val u = Uri.parse(raw)
            val sid = u.getQueryParameter("sid") ?: u.getQueryParameter("sessionId")
            if (!sid.isNullOrBlank()) return sid
        }

        return null
    }
}
