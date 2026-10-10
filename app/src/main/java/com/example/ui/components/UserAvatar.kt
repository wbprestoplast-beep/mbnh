package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal

@Composable
fun UserAvatar(
    name: String,
    photoUri: String? = null,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "?" }

    val decodedBitmap = remember(photoUri) {
        if (!photoUri.isNullOrBlank() && (photoUri.startsWith("data:image") || (photoUri.length > 100 && !photoUri.startsWith("/") && !photoUri.startsWith("http") && !photoUri.startsWith("content:")))) {
            try {
                val raw = if (photoUri.contains(",")) photoUri.substringAfter(",") else photoUri
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(BrandCyan, BrandTeal)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            !photoUri.isNullOrBlank() -> {
                AsyncImage(
                    model = photoUri,
                    contentDescription = name,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
