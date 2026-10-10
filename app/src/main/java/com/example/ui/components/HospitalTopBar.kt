package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.model.AppScreen
import com.example.model.UserRole
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalTopBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    unreadNotificationCount: Int,
    currentUser: UserEntity?,
    onBackClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onClearNotificationsClick: (() -> Unit)? = null
) {
    val isBoss = currentUser?.role?.equals("BOSS", ignoreCase = true) == true
    val screenTitle = when (currentScreen) {
        AppScreen.HOME -> "MB Nursing Home"
        AppScreen.PATIENTS -> "Patients"
        AppScreen.PATIENT_DETAIL -> "Patient Record"
        AppScreen.ADMIT_PATIENT -> "Admit Patient"
        AppScreen.BEDS -> "Bed Occupancy"
        AppScreen.ATTENDANCE -> "Broadcasts & Alerts"
        AppScreen.PLANNINGS -> "Plannings & Tasks"
        AppScreen.ADMIN -> if (isBoss) "Boss · Admin Portal" else if (currentUser?.role?.contains("ADMIN", ignoreCase = true) == true) "Administrator Portal" else "Hospital Staff & Doctors"
        AppScreen.NOTIFICATIONS -> "Broadcasts & Notifications"
        AppScreen.SETTINGS -> "Settings"
        else -> "MB Nursing Home"
    }

    TopAppBar(
        title = {
            if (currentScreen == AppScreen.HOME) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hospital_logo),
                        contentDescription = "Company Logo",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MB Nursing Home",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = screenTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hospital_logo),
                        contentDescription = "Company Logo",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        },
        actions = {
            if (currentScreen == AppScreen.NOTIFICATIONS && onClearNotificationsClick != null) {
                OutlinedButton(
                    onClick = onClearNotificationsClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("topbar_clear_notifications_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear notifications and updates",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("topbar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadNotificationCount", fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (currentUser != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onProfileClick() }
                ) {
                    UserAvatar(
                        name = currentUser.name,
                        photoUri = currentUser.photoUri,
                        size = 34.dp
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    )
}
