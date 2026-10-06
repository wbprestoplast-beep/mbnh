package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fcm.FcmManager
import com.example.model.AppScreen
import com.example.model.UserRole
import com.example.ui.components.HospitalBottomNav
import com.example.ui.components.HospitalTopBar
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AdmitPatientScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BedsScreen
import com.example.ui.screens.DocumentScannerModal
import com.example.ui.screens.DocumentViewerModal
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PatientDetailScreen
import com.example.ui.screens.PatientsScreen
import com.example.ui.screens.PlanningsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HospitalTheme
import com.example.ui.viewmodel.HospitalViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase Cloud Messaging & Notification channels
        FcmManager.initialize(applicationContext)

        setContent {
            val viewModel: HospitalViewModel = viewModel()
            val themeSetting by viewModel.themeSetting.collectAsState()

            HospitalTheme(themeSetting = themeSetting) {
                HospitalApp(viewModel)
            }
        }
    }
}

@Composable
fun HospitalApp(viewModel: HospitalViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val screenStack by viewModel.screenStack.collectAsState()
    val currentScreen = screenStack.lastOrNull() ?: AppScreen.HOME
    val canNavigateBack = screenStack.size > 1

    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count {
        !it.read && (it.audience == "all" || it.audience == currentUser?.id || it.audience.equals(currentUser?.role, ignoreCase = true))
    }

    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isScannerOpen by viewModel.scannerOpen.collectAsState()
    val selectedDocument by viewModel.selectedDocument.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()

    // Request notification permission on Android 13+ (API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showToast("🔔 Push notifications enabled")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Subscribe user to role-based topic when logged in
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val roleTopic = "role_${user.role.lowercase()}"
            val userTopic = "user_${user.id.lowercase().replace('-', '_')}"
            FcmManager.subscribeToTopic(roleTopic)
            FcmManager.subscribeToTopic(userTopic)
        }
    }

    // Handle back button on secondary screens
    BackHandler(enabled = canNavigateBack) {
        viewModel.navigateBack()
    }

    // Handle Toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HospitalTopBar(
                    currentScreen = currentScreen,
                    canNavigateBack = canNavigateBack,
                    unreadNotificationCount = unreadCount,
                    currentUser = currentUser,
                    onBackClick = { viewModel.navigateBack() },
                    onNotificationsClick = { viewModel.pushScreen(AppScreen.NOTIFICATIONS) },
                    onProfileClick = { viewModel.pushScreen(AppScreen.SETTINGS) }
                )
            },
            bottomBar = {
                HospitalBottomNav(
                    userRole = userRole,
                    currentScreen = currentScreen,
                    onTabSelected = { targetScreen ->
                        viewModel.navigateTo(targetScreen)
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.PATIENTS -> PatientsScreen(viewModel = viewModel)
                    AppScreen.PATIENT_DETAIL -> {
                        PatientDetailScreen(
                            patientId = selectedPatientId ?: "",
                            viewModel = viewModel
                        )
                    }
                    AppScreen.ADMIT_PATIENT -> AdmitPatientScreen(viewModel = viewModel)
                    AppScreen.BEDS -> BedsScreen(viewModel = viewModel)
                    AppScreen.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                    AppScreen.PLANNINGS -> PlanningsScreen(viewModel = viewModel)
                    AppScreen.ADMIN -> AdminPortalScreen(viewModel = viewModel)
                    AppScreen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    else -> HomeScreen(viewModel = viewModel)
                }
            }

            // Document Scanner Modal
            if (isScannerOpen) {
                DocumentScannerModal(viewModel = viewModel)
            }

            // Document Viewer Modal
            if (selectedDocument != null) {
                DocumentViewerModal(
                    document = selectedDocument!!,
                    viewModel = viewModel
                )
            }
        }
    }
}
