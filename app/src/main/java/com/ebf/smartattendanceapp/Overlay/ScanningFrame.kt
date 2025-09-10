package com.ebf.smartattendanceapp.Overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScanningFrame() {
    Canvas(modifier = Modifier.size(280.dp)) {
        val strokeWidth = 8f
        val cornerLength = size.width / 4

        // Draw the four corners of the scanning box
        // Top-left
        drawLine(Color.White, start = Offset(0f, cornerLength), end = Offset(0f, 0f), strokeWidth)
        drawLine(Color.White, start = Offset(0f, 0f), end = Offset(cornerLength, 0f), strokeWidth)
        // Top-right
        drawLine(Color.White, start = Offset(size.width - cornerLength, 0f), end = Offset(size.width, 0f), strokeWidth)
        drawLine(Color.White, start = Offset(size.width, 0f), end = Offset(size.width, cornerLength), strokeWidth)
        // Bottom-left
        drawLine(Color.White, start = Offset(0f, size.height - cornerLength), end = Offset(0f, size.height), strokeWidth)
        drawLine(Color.White, start = Offset(0f, size.height), end = Offset(cornerLength, size.height), strokeWidth)
        // Bottom-right
        drawLine(Color.White, start = Offset(size.width - cornerLength, size.height), end = Offset(size.width, size.height), strokeWidth)
        drawLine(Color.White, start = Offset(size.width, size.height), end = Offset(size.width, size.height - cornerLength), strokeWidth)
    }
}