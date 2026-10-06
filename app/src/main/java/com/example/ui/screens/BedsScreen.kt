package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HospitalConstants
import com.example.model.UserRole
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.viewmodel.HospitalViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BedsScreen(viewModel: HospitalViewModel) {
    val patients by viewModel.patients.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isDoctor = userRole == UserRole.DOCTOR
    val doctors = users.filter { it.role.equals("DOCTOR", ignoreCase = true) }

    val totalBeds = HospitalConstants.WARDS.sumOf { it.beds.size }
    val occupiedBeds = patients.size
    val availableBeds = totalBeds - occupiedBeds
    val occupancyPercent = if (totalBeds > 0) (occupiedBeds.toFloat() / totalBeds.toFloat()) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall Occupancy Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$occupiedBeds",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandTeal
                                )
                                Text(
                                    text = " of $totalBeds occupied",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                )
                            }
                            Text(
                                text = "$availableBeds beds currently available",
                                fontSize = 12.5.sp,
                                color = MedGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandTeal.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${(occupancyPercent * 100).toInt()}% Full",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { occupancyPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = BrandCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Doctor Occupancy Breakdown (visible to all or non-doctors)
        if (!isDoctor) {
            item {
                Text(
                    text = "OCCUPANCY UNDER EACH DOCTOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    doctors.forEach { doc ->
                        val docPatients = patients.filter { it.doctorId == doc.id }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(name = doc.name, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${doc.specialty} · Beds: ${docPatients.map { it.bed }.joinToString().ifEmpty { "None" }}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(BrandTeal.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${docPatients.size} occupied",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ward-by-Ward Interactive Bed Grid
        HospitalConstants.WARDS.forEach { ward ->
            val wardPatients = patients.filter { it.ward == ward.name }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ward.name.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "${wardPatients.size}/${ward.beds.size} occupied",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    ward.beds.forEach { bedCode ->
                        val pt = patients.firstOrNull { it.bed == bedCode }
                        val isOccupied = pt != null
                        val isDoctorRestricted = isDoctor && pt != null && (pt.doctorId != currentUser?.id && pt.referralDoctorId != currentUser?.id)
                        val attendingDoc = pt?.let { p -> users.firstOrNull { it.id == p.doctorId } }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isOccupied) BrandCyan.copy(alpha = 0.08f)
                                    else MedGreen.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.5.dp,
                                    if (isOccupied) BrandCyan.copy(alpha = 0.45f)
                                    else MedGreen.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (pt != null) {
                                        if (isDoctorRestricted) {
                                            viewModel.showToast("🔒 Patient details restricted to assigned doctor")
                                        } else {
                                            viewModel.openPatientDetail(pt.id)
                                        }
                                    } else {
                                        viewModel.showToast("Bed $bedCode is available")
                                    }
                                }
                                .padding(8.dp)
                                .testTag("bed_card_$bedCode")
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bedCode,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isOccupied) BrandCyan else MedGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                if (isOccupied && pt != null) {
                                    if (isDoctorRestricted) {
                                        Text(
                                            text = "Occupied\nRestricted",
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = pt.name,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = attendingDoc?.name?.replace("Dr. ", "Dr.") ?: "",
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
