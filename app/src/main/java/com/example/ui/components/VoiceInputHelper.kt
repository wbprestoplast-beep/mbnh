package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.BrandTeal
import java.util.Locale

/**
 * Reusable Voice-to-Text Icon Button.
 * Launches Android Speech Recognizer, or falls back to clinical voice dictation modal if not installed.
 */
@Composable
fun VoiceToTextIconButton(
    onTextSpoken: (String) -> Unit,
    modifier: Modifier = Modifier,
    prompt: String = "Speak now to convert voice to text...",
    tint: Color = BrandTeal
) {
    val context = LocalContext.current
    var showFallbackDialog by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                onTextSpoken(spokenText)
                Toast.makeText(context, "🎙️ Dictated: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSystemSpeech(context, prompt, speechRecognizerLauncher) {
                showFallbackDialog = true
            }
        } else {
            showFallbackDialog = true
        }
    }

    IconButton(
        onClick = {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                launchSystemSpeech(context, prompt, speechRecognizerLauncher) {
                    showFallbackDialog = true
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier
            .size(36.dp)
            .testTag("voice_to_text_mic_button")
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice to text dictation",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }

    if (showFallbackDialog) {
        VoiceDictationModal(
            prompt = prompt,
            onDismiss = { showFallbackDialog = false },
            onSelectText = { text ->
                onTextSpoken(text)
                showFallbackDialog = false
            }
        )
    }
}

private fun launchSystemSpeech(
    context: Context,
    prompt: String,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onFallback: () -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        launcher.launch(intent)
    } catch (_: Exception) {
        onFallback()
    }
}

/**
 * Interactive Voice Dictation Dialog with pulsing microphone, custom typing, and clinical preset shortcuts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceDictationModal(
    prompt: String,
    onDismiss: () -> Unit,
    onSelectText: (String) -> Unit
) {
    var dictationInput by remember { mutableStateOf("") }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isPasswordPrompt = prompt.contains("password", ignoreCase = true)
    val isIdPrompt = prompt.contains("id", ignoreCase = true) || prompt.contains("user", ignoreCase = true)
    val isSearchPrompt = prompt.contains("search", ignoreCase = true)
    val isPhonePrompt = prompt.contains("phone", ignoreCase = true)

    val activePresets = when {
        isPasswordPrompt -> listOf("12345", "hospital2026", "admin123", "pass2026")
        isIdPrompt -> listOf("DOC-2001", "NUR-3001", "REC-4001", "ADM-1001", "BOSS-0001", "RMO-2002")
        isSearchPrompt -> listOf("Emergency", "Cardiology", "ICU Ward", "General Ward", "Dr. Sarah", "Dr. Rajesh", "Admitted")
        isPhonePrompt -> listOf("+1 555-0199", "+1 555-0144", "+1 555-0182", "+1 555-0120")
        else -> listOf(
            "Stable vitals and normal clinical findings",
            "Patient reviewing well under current treatment",
            "Prescription charted and administered by nursing staff",
            "Post-op dressing clean and intact",
            "Urgent consultant doctor review required",
            "Bedside vital monitoring completed",
            "Transferred to general ward",
            "Scheduled for routine diagnostic blood panel",
            "Patient discharged in stable condition"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ Voice to Text Dictation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pulsing Mic Visual
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(BrandTeal.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(BrandTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = prompt,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = dictationInput,
                    onValueChange = { dictationInput = it },
                    placeholder = { Text("Speak or type text to insert...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Voice Presets
                Text(
                    text = "Quick Voice Presets (Tap to insert):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activePresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    dictationInput = if (dictationInput.isBlank()) preset else "$dictationInput $preset"
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (dictationInput.isNotBlank()) {
                            onSelectText(dictationInput.trim())
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = dictationInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Insert Voice Text", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Drop-in OutlinedTextField replacement with built-in voice-to-text mic icon.
 */
@Composable
fun VoiceOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    customTrailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    speechPrompt: String = "Speak now to enter text...",
    appendOnSpeech: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val wrappedKeyboardActions = remember(keyboardActions, keyboardController, focusManager) {
        KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
                keyboardActions.onDone?.invoke(this)
            },
            onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
                keyboardActions.onSearch?.invoke(this)
            },
            onNext = {
                if (keyboardActions.onNext != null) {
                    keyboardActions.onNext?.invoke(this)
                } else {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            },
            onGo = {
                focusManager.clearFocus()
                keyboardController?.hide()
                keyboardActions.onGo?.invoke(this)
            },
            onSend = {
                focusManager.clearFocus()
                keyboardController?.hide()
                keyboardActions.onSend?.invoke(this)
            },
            onPrevious = {
                if (keyboardActions.onPrevious != null) {
                    keyboardActions.onPrevious?.invoke(this)
                } else {
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            }
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        supportingText = supportingText,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                customTrailingIcon?.invoke()
                VoiceToTextIconButton(
                    onTextSpoken = { spoken ->
                        if (appendOnSpeech && value.isNotBlank()) {
                            onValueChange("$value $spoken")
                        } else {
                            onValueChange(spoken)
                        }
                    },
                    prompt = speechPrompt
                )
            }
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = wrappedKeyboardActions,
        readOnly = readOnly,
        enabled = enabled,
        shape = shape,
        colors = colors
    )
}
