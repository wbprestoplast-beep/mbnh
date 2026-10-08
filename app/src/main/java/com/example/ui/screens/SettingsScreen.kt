package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppScreen
import com.example.model.UserRole
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AppThemeSetting
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@Composable
fun SettingsScreen(viewModel: HospitalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val themeSetting by viewModel.themeSetting.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isBoss = userRole == UserRole.BOSS || currentUser?.id == "BOSS-0001"

    var showEditProfileModal by remember { mutableStateOf(false) }
    var showChangePasswordModal by remember { mutableStateOf(false) }
    var showDeleteAccountModal by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && currentUser != null) {
            viewModel.updateUserPhoto(currentUser!!.id, uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile Card
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = currentUser?.name ?: "User",
                            photoUri = currentUser?.photoUri,
                            size = 62.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentUser?.name ?: "Staff Member",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isBoss) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(MedWarning.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("👑 BOSS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MedWarning)
                                    }
                                }
                            }
                            Text(
                                text = "${userRole.label} · ID: ${currentUser?.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTeal,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "📞 ${if (!currentUser?.phone.isNullOrBlank()) currentUser?.phone else "No phone set"}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change profile picture",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showEditProfileModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Name & Phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showChangePasswordModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Password", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Boss Super-Admin Rights Card (Shown only to Boss)
        if (isBoss) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MedWarning.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👑", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Boss Master Control & Credential Vault", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MedWarning)
                                Text("You have full access to view all staff passwords & IDs, edit names, profiles, pictures, phone numbers, and rights.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.pushScreen(AppScreen.ADMIN) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedWarning),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Boss Roster & View All Passwords", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }

        // Section: Profile & Security Information
        item {
            Text(
                text = "IDENTITY & CREDENTIALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // User ID row (Locked for regular users)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("User ID", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(currentUser?.id ?: "—", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = BrandTeal)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔒 Assigned by Boss & Admin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Password row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Password", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("•••••••• (Encrypted on device)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = { showChangePasswordModal = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Phone Number row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Contact Number", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(currentUser?.phone?.ifBlank { "Not configured" } ?: "Not configured", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = { showEditProfileModal = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Edit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Appearance
        item {
            Text(
                text = "APPEARANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Theme Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose light, dark, or system setting",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            AppThemeSetting.AUTO to "Auto",
                            AppThemeSetting.LIGHT to "Light",
                            AppThemeSetting.DARK to "Dark"
                        ).forEach { (setting, label) ->
                            FilterChip(
                                selected = themeSetting == setting,
                                onClick = { viewModel.setThemeSetting(setting) },
                                label = { Text(label, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: System & Integration Status
        item {
            Text(
                text = "SYSTEM & INTEGRATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MedGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = MedGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline SQLite Room Cache", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Local persistence active · Instant offline functionality", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MedGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedGreen)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Real-Time Multi-Device Cloud Sync", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Firestore live streams & background alert notifications", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MedGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedGreen)
                        }
                    }
                }
            }
        }

        // Section: Account Actions
        item {
            Text(
                text = "ACCOUNT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isBoss) {
                                    viewModel.showToast("⚠️ Boss account cannot be deleted")
                                } else {
                                    showDeleteAccountModal = true
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete Account", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Permanently removes your profile and login ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { viewModel.logout() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of HMS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "MB Nursing Home Pvt. Ltd. · Hospital Management System v2.0\n12/3 M.G. Road, Kolkata · Ph: 033-2456-7890",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    // Edit Profile Modal (Name & Phone Number)
    if (showEditProfileModal) {
        var nameInput by remember { mutableStateOf(currentUser?.name ?: "") }
        var phoneInput by remember { mutableStateOf(currentUser?.phone ?: "") }
        var emailInput by remember { mutableStateOf(currentUser?.email ?: "") }

        Dialog(onDismissRequest = { showEditProfileModal = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Edit My Profile Details",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Update your display name and contact phone number.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    // Read-only User ID field
                    OutlinedTextField(
                        value = currentUser?.id ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("User ID (Locked)") },
                        supportingText = { Text("User ID is configured exclusively by Boss & Admin", fontSize = 10.5.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Contact Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    viewModel.updateOwnProfile(nameInput, phoneInput, emailInput)
                                    showEditProfileModal = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            enabled = nameInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Profile", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showEditProfileModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Change Password Modal
    if (showChangePasswordModal) {
        var currentPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }
        var showPassword by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showChangePasswordModal = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Change Account Password",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Create a new private passcode to sign in to your HMS account.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it; passError = null },
                        label = { Text("Current Password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it; passError = null },
                        label = { Text("New Password *") },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it; passError = null },
                        label = { Text("Confirm New Password *") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passError != null) {
                        Text(
                            text = "⚠ $passError",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newPassInput.trim().length < 3) {
                                    passError = "Password must be at least 3 characters"
                                    return@Button
                                }
                                if (newPassInput != confirmPassInput) {
                                    passError = "New passwords do not match"
                                    return@Button
                                }
                                val ok = viewModel.updateOwnPassword(currentPassInput.trim(), newPassInput.trim())
                                if (ok) {
                                    showChangePasswordModal = false
                                } else {
                                    passError = "Current password incorrect"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            enabled = newPassInput.isNotBlank() && confirmPassInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Password", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showChangePasswordModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountModal) {
        Dialog(onDismissRequest = { showDeleteAccountModal = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚠ Delete Account Permanently?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Consequences:\n• Login ID ${currentUser?.id} will be permanently deactivated.\n• Profile details and attendance will be removed.\n• Patients assigned to you will require re-assignment by the Administrator.\n• This action cannot be undone.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showDeleteAccountModal = false
                                viewModel.deleteCurrentAccount()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                        OutlinedButton(
                            onClick = { showDeleteAccountModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
