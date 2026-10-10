package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import com.example.ui.components.VoiceOutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.UserRole
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.viewmodel.HospitalViewModel

@Composable
fun LoginScreen(viewModel: HospitalViewModel) {
    val users by viewModel.users.collectAsState()

    val savedId = viewModel.savedUserId
    val savedPass = viewModel.savedPassword

    var userId by remember { mutableStateOf(savedId) }
    var password by remember { mutableStateOf(savedPass) }
    var rememberCredentials by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Auto-reconcile with cloud on Login Screen open and populate remembered credentials
    LaunchedEffect(savedId, savedPass) {
        if (savedId.isNotBlank() && userId.isBlank()) {
            userId = savedId
        }
        if (savedPass.isNotBlank() && password.isBlank()) {
            password = savedPass
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncAllDevicesImmediately()
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitLogin() {
        focusManager.clearFocus()
        keyboardController?.hide()
        val cleanUser = userId.trim()
        val cleanPass = password.trim()
        if (cleanUser.isBlank()) {
            errorMessage = "Please enter your User ID (e.g. DOC-2001, RMO-2101, MNT-4401)"
            return
        }
        val passToTry = cleanPass.ifBlank { "12345" }
        isLoggingIn = true
        errorMessage = null
        viewModel.loginWithCloudFallback(cleanUser, passToTry, autoSave = rememberCredentials) { success, err ->
            isLoggingIn = false
            if (!success) {
                errorMessage = err ?: "Invalid User ID or Password. Default password for hospital staff is 12345."
            } else {
                errorMessage = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hospital Logo Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hospital_logo),
                        contentDescription = "Hospital Company Logo",
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
                        text = "MB Nursing Home Pvt. Ltd.",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "Hospital Management System · Staff & Doctors",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 18.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )

                VoiceOutlinedTextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                        errorMessage = null
                    },
                    label = { Text("User ID (e.g. DOC-2001)") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "User ID")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    speechPrompt = "Speak your User ID (e.g. DOC 2001 or BOSS 0001)...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_user_id_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                VoiceOutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password")
                    },
                    customTrailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    speechPrompt = "Speak your password to convert voice to text...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                // Row with Auto-Save Credentials & Forgot Password Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberCredentials = !rememberCredentials }
                    ) {
                        Icon(
                            imageVector = if (rememberCredentials) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                            contentDescription = "Save credentials",
                            tint = if (rememberCredentials) MedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (rememberCredentials) "Credentials auto-saved on device" else "Save on device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (rememberCredentials) MedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Forgot Password?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        modifier = Modifier
                            .clickable {
                                showForgotPasswordDialog = true
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("forgot_password_button")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = "⚠ $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { submitLogin() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    enabled = !isLoggingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking Hospital Cloud...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Log In",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log In to HMS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "🔒 Secure internal hospital management system. Credentials are auto-saved on device.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Forgot Password Modal Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialUserId = userId,
            users = users,
            onDismiss = { showForgotPasswordDialog = false },
            onPasswordReset = { targetUserId, newPass ->
                viewModel.resetPassword(targetUserId, newPass)
                userId = targetUserId
                password = newPass
                errorMessage = null
                showForgotPasswordDialog = false
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialUserId: String,
    users: List<com.example.data.UserEntity>,
    onDismiss: () -> Unit,
    onPasswordReset: (String, String) -> Unit
) {
    var searchId by remember { mutableStateOf(initialUserId) }
    var newPassword by remember { mutableStateOf("12345") }
    var confirmPassword by remember { mutableStateOf("12345") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val matchedUser = users.firstOrNull { it.id.equals(searchId.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Reset Password",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Verify your User ID to reset your hospital login credentials.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // User ID Input
                VoiceOutlinedTextField(
                    value = searchId,
                    onValueChange = {
                        searchId = it
                        validationError = null
                    },
                    label = { Text("Enter your User ID *") },
                    placeholder = { Text("e.g. DOC-2001, BOSS-0001") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    speechPrompt = "Speak your User ID to verify...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_password_user_id_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // User Identity Verification Card
                if (matchedUser != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MedGreen.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(name = matchedUser.name, photoUri = matchedUser.photoUri, size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = matchedUser.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = MedGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "${UserRole.fromKey(matchedUser.role).label} · ${matchedUser.dept.ifBlank { matchedUser.specialty }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (searchId.isNotBlank()) {
                    Text(
                        text = "User ID '$searchId' not found. Check ID or contact Admin below.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // New Password Field
                VoiceOutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        validationError = null
                    },
                    label = { Text("New Password *") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    customTrailingIcon = {
                        IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                            Icon(
                                imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    speechPrompt = "Speak your new password...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Convenient "Use Default 12345" chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandCyan.copy(alpha = 0.15f))
                            .clickable {
                                newPassword = "12345"
                                confirmPassword = "12345"
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Set to Default (12345)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm Password Field
                VoiceOutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        validationError = null
                    },
                    label = { Text("Confirm New Password *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    speechPrompt = "Speak to confirm your new password...",
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = "⚠ $validationError",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Reset Button
                Button(
                    onClick = {
                        when {
                            matchedUser == null -> {
                                validationError = "Please enter a valid User ID"
                            }
                            newPassword.isBlank() -> {
                                validationError = "New password cannot be empty"
                            }
                            newPassword != confirmPassword -> {
                                validationError = "Passwords do not match"
                            }
                            else -> {
                                onPasswordReset(matchedUser.id, newPassword)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    enabled = matchedUser != null && newPassword.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_reset_password_button")
                ) {
                    Text("Confirm & Reset Password", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Help & Contact Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hospital Admin Support",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Contact Admin: 03340198989, 9073364305",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
