package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AvailabilityStatus
import com.example.domain.model.RequestStatus

@Composable
fun AvailabilityStatusBadge(status: AvailabilityStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        AvailabilityStatus.AVAILABLE -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "AVAILABLE")
        AvailabilityStatus.BOOKED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "BOOKED")
        AvailabilityStatus.PICKED_UP -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "PICKED UP")
        AvailabilityStatus.EXPIRED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "EXPIRED")
        AvailabilityStatus.REMOVED -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), "REMOVED")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RequestStatusBadge(status: RequestStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        RequestStatus.PENDING -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "PENDING")
        RequestStatus.ACCEPTED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "ACCEPTED")
        RequestStatus.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "COMPLETED")
        RequestStatus.REJECTED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "CLOSED")
        RequestStatus.WITHDRAWN -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), "WITHDRAWN")
        RequestStatus.EXPIRED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "EXPIRED")
        RequestStatus.CANCELLED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "CANCELLED")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
