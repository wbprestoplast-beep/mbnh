package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.AppScreen
import com.example.model.UserRole
import com.example.ui.theme.BrandTeal

data class NavTabItem(
    val screen: AppScreen,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

@Composable
fun HospitalBottomNav(
    userRole: UserRole,
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    val tabs = when (userRole) {
        UserRole.BOSS -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.BEDS, "Beds", Icons.Filled.Hotel, Icons.Outlined.Hotel),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign),
            NavTabItem(AppScreen.PLANNINGS, "Plannings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
        )
        UserRole.RMO, UserRole.RMO_INCHARGE -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.ADMIN, "Staff", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.BEDS, "Beds", Icons.Filled.Hotel, Icons.Outlined.Hotel),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
        UserRole.DOCTOR, UserRole.MEDICAL_SUPER -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.BEDS, "Beds", Icons.Filled.Hotel, Icons.Outlined.Hotel),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign),
            NavTabItem(AppScreen.PLANNINGS, "Plannings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
        )
        UserRole.ADMINISTRATOR -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.ADMIN, "Staff", Icons.Filled.PersonAdd, Icons.Outlined.PersonAdd),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
        UserRole.RECEPTIONIST, UserRole.MAINTENANCE, UserRole.ACCOUNTANT, UserRole.CASHIER -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.ADMIN, "Staff", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
        UserRole.INCHARGE -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.ADMIN, "Staff", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.PLANNINGS, "Tasks", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
        UserRole.NURSE, UserRole.TECHNICIAN -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.PLANNINGS, "Tasks", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
        else -> listOf(
            NavTabItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTabItem(AppScreen.PATIENTS, "Patients", Icons.Filled.People, Icons.Outlined.People),
            NavTabItem(AppScreen.NOTIFICATIONS, "Broadcasts", Icons.Filled.Campaign, Icons.Outlined.Campaign)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        tabs.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandTeal,
                    selectedTextColor = BrandTeal,
                    indicatorColor = BrandTeal.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${item.label.lowercase()}")
            )
        }
    }
}
