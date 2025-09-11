package com.ebf.smartattendanceapp.Indicators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusIndicator(iconId: Int, text: String) {
    Icon(painter = painterResource(id = iconId), contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
    Spacer(modifier = Modifier.height(24.dp))
    Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center , modifier = Modifier.padding(top = 400.dp))
}