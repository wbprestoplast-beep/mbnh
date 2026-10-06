package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DocumentFilter

@Composable
fun DocumentCanvasView(
    docTypeOrUri: String,
    filter: DocumentFilter = DocumentFilter.ORIGINAL,
    modifier: Modifier = Modifier
) {
    val colorFilter = when (filter) {
        DocumentFilter.ORIGINAL -> null
        DocumentFilter.BW_CONTRAST -> {
            val matrix = ColorMatrix().apply {
                setToSaturation(0f)
                // Boost contrast
                val scale = 1.8f
                val translate = (-0.5f * scale + 0.5f) * 255f
                set(0, 0, scale)
                set(1, 1, scale)
                set(2, 2, scale)
                set(0, 4, translate)
                set(1, 4, translate)
                set(2, 4, translate)
            }
            ColorFilter.colorMatrix(matrix)
        }
        DocumentFilter.GRAYSCALE -> {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
        DocumentFilter.BOOST -> {
            val matrix = ColorMatrix().apply {
                setToSaturation(1.3f)
                val scale = 1.25f
                val translate = 10f
                set(0, 0, scale)
                set(1, 1, scale)
                set(2, 2, scale)
                set(0, 4, translate)
                set(1, 4, translate)
                set(2, 4, translate)
            }
            ColorFilter.colorMatrix(matrix)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        when (docTypeOrUri) {
            "SEED_RX" -> RxDocumentView(modifier = Modifier.fillMaxSize())
            "SEED_LAB" -> LabDocumentView(modifier = Modifier.fillMaxSize())
            "SEED_XRAY" -> XRayDocumentView(modifier = Modifier.fillMaxSize())
            "SEED_MRI" -> MriDocumentView(modifier = Modifier.fillMaxSize())
            else -> {
                AsyncImage(
                    model = docTypeOrUri,
                    contentDescription = "Medical Document",
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun RxDocumentView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F7FA), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = "MB NURSING HOME PVT. LTD.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0E7490)
                )
                Text(
                    text = "Dr. Arif Khan · MD, DM (Cardiology) · Reg. WB-442211",
                    fontSize = 8.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rx",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = Color(0xFF0E7490)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "OPD / IPD CLINICAL RECORD",
                fontSize = 7.5.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 4.dp)
        ) {
            val lines = 4
            val stepY = size.height / (lines + 1)
            for (i in 1..lines) {
                val y = i * stepY
                val path = Path().apply {
                    moveTo(0f, y)
                    quadraticBezierTo(size.width * 0.25f, y - 4f, size.width * 0.5f, y + 2f)
                    quadraticBezierTo(size.width * 0.75f, y - 3f, size.width, y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF1E293B),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Follow up after 14 days",
                fontSize = 8.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Dr. A. Khan [Signed]",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E7490)
            )
        }
    }
}

@Composable
fun LabDocumentView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E7490), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Column {
                Text(
                    text = "MB NURSING HOME — PATHOLOGY LAB",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Complete Blood Count · Biochemistry Panel",
                    fontSize = 7.5.sp,
                    color = Color(0xFFCFFAFE)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text("Test", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("Result", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Ref", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        }

        val rows = listOf(
            Triple("Haemoglobin", "9.4 g/dL ▼", "13.0–17.0"),
            Triple("Total WBC", "14,900 /µL ▲", "4k–11k"),
            Triple("Platelets", "2.6 lakh", "1.5–4.0L"),
            Triple("Fasting Glucose", "128 mg/dL ▲", "70–100"),
            Triple("Total Cholesterol", "232 mg/dL ▲", "< 200")
        )

        rows.forEach { (t, r, ref) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 1.5.dp)
            ) {
                Text(t, fontSize = 7.sp, modifier = Modifier.weight(1.2f), color = Color(0xFF334155))
                val isAbnormal = r.contains("▲") || r.contains("▼")
                Text(
                    r,
                    fontSize = 7.sp,
                    fontWeight = if (isAbnormal) FontWeight.Bold else FontWeight.Normal,
                    color = if (isAbnormal) Color(0xFFB91C1C) else Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )
                Text(ref, fontSize = 7.sp, color = Color(0xFF64748B), modifier = Modifier.weight(0.8f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Text(
            text = "Impression: Anaemia with leucocytosis. Dr. S. Banerjee, MD (Path)",
            fontSize = 7.sp,
            color = Color(0xFF475569),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun XRayDocumentView(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.background(Color(0xFF070B14))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Spine
        drawRoundRect(
            color = Color(0xFF5C6B82),
            topLeft = Offset(cx - size.width * 0.03f, size.height * 0.15f),
            size = Size(size.width * 0.06f, size.height * 0.65f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // Left and Right Lungs
        val lungWidth = size.width * 0.28f
        val lungHeight = size.height * 0.52f
        drawOval(
            color = Color(0xFF2B3646).copy(alpha = 0.85f),
            topLeft = Offset(cx - lungWidth - size.width * 0.05f, cy - lungHeight * 0.45f),
            size = Size(lungWidth, lungHeight)
        )
        drawOval(
            color = Color(0xFF2B3646).copy(alpha = 0.85f),
            topLeft = Offset(cx + size.width * 0.05f, cy - lungHeight * 0.45f),
            size = Size(lungWidth, lungHeight)
        )

        // Ribs arches
        val ribCount = 6
        for (i in 1..ribCount) {
            val yOffset = cy - lungHeight * 0.35f + i * (lungHeight * 0.7f / ribCount)
            val leftRib = Path().apply {
                moveTo(cx - 10f, yOffset)
                quadraticBezierTo(cx - lungWidth * 0.7f, yOffset - 15f, cx - lungWidth * 1.05f, yOffset + 20f)
            }
            val rightRib = Path().apply {
                moveTo(cx + 10f, yOffset)
                quadraticBezierTo(cx + lungWidth * 0.7f, yOffset - 15f, cx + lungWidth * 1.05f, yOffset + 20f)
            }
            drawPath(leftRib, Color(0xFF5C6B82).copy(alpha = 0.8f), style = Stroke(width = 3.5f))
            drawPath(rightRib, Color(0xFF5C6B82).copy(alpha = 0.8f), style = Stroke(width = 3.5f))
        }

        // Heart shadow
        drawOval(
            color = Color(0xFF39445A).copy(alpha = 0.85f),
            topLeft = Offset(cx - lungWidth * 0.4f, cy + lungHeight * 0.1f),
            size = Size(lungWidth * 0.9f, lungHeight * 0.35f)
        )
    }
}

@Composable
fun MriDocumentView(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.background(Color(0xFF05070C))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rOuter = size.minDimension * 0.42f
        val rMid = size.minDimension * 0.36f
        val rInner = size.minDimension * 0.31f

        drawOval(
            color = Color(0xFF1C2430),
            topLeft = Offset(cx - rOuter, cy - rOuter * 1.15f),
            size = Size(rOuter * 2f, rOuter * 2.3f)
        )
        drawOval(
            color = Color(0xFF8D99A8).copy(alpha = 0.75f),
            topLeft = Offset(cx - rMid, cy - rMid * 1.15f),
            size = Size(rMid * 2f, rMid * 2.3f)
        )
        drawOval(
            color = Color(0xFFC3CCD6).copy(alpha = 0.85f),
            topLeft = Offset(cx - rInner, cy - rInner * 1.15f),
            size = Size(rInner * 2f, rInner * 2.3f)
        )

        // Midline
        drawLine(
            color = Color(0xFF7D8794),
            start = Offset(cx, cy - rOuter),
            end = Offset(cx, cy + rOuter),
            strokeWidth = 3f
        )

        // Ventricles
        drawOval(
            color = Color(0xFFE8EDF2),
            topLeft = Offset(cx - 16f, cy - 25f),
            size = Size(32f, 50f)
        )

        // Lesion marker
        drawCircle(
            color = Color(0xFFF87171).copy(alpha = 0.8f),
            center = Offset(cx + rInner * 0.4f, cy - rInner * 0.25f),
            radius = 12f
        )
    }
}
