package com.example.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PinLockManager

private enum class PinScreenState {
    ENTER_CURRENT_PIN,
    RESET_NEW_PIN_STEP_1,
    RESET_NEW_PIN_STEP_2
}

@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Prevent bypassing the lock screen: back press will exit/finish the app
    BackHandler(enabled = true) {
        activity?.finish()
    }

    var screenState by remember { mutableStateOf(PinScreenState.ENTER_CURRENT_PIN) }
    var enteredPin by remember { mutableStateOf("") }
    var temporaryNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showNoDeviceSecurityDialog by remember { mutableStateOf(false) }

    // Launcher for Android Phone Screen Lock / Credential Verification (PIN, Pattern, Password, Biometrics)
    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User successfully authenticated using Phone Device Lock!
            Toast.makeText(context, "تم التحقق من قفل الهاتف بنجاح 🔓 يرجى تعيين رمز PIN جديد للتطبيق", Toast.LENGTH_LONG).show()
            enteredPin = ""
            temporaryNewPin = ""
            errorMessage = null
            screenState = PinScreenState.RESET_NEW_PIN_STEP_1
        } else {
            errorMessage = "تم إلغاء التحقق بقفل الهاتف"
        }
    }

    fun requestDeviceCredentialUnlock() {
        try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager?.isDeviceSecure == true) {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "تأكيد هوية المعلم 🔒",
                    "أدخل قفل الهاتف (PIN / النمط / كلمة المرور / البصمة) للمتابعة وتعيين رمز جديد للتطبيق"
                )
                if (intent != null) {
                    deviceCredentialLauncher.launch(intent)
                } else {
                    showNoDeviceSecurityDialog = true
                }
            } else {
                showNoDeviceSecurityDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showNoDeviceSecurityDialog = true
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("pin_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Header Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (screenState == PinScreenState.ENTER_CURRENT_PIN) NavyPrimary else Color(0xFFD97706),
                                if (screenState == PinScreenState.ENTER_CURRENT_PIN) Color(0xFF1E40AF) else AmberGold
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (screenState) {
                        PinScreenState.ENTER_CURRENT_PIN -> Icons.Filled.Lock
                        PinScreenState.RESET_NEW_PIN_STEP_1 -> Icons.Filled.VpnKey
                        PinScreenState.RESET_NEW_PIN_STEP_2 -> Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Instruction based on state
            AnimatedContent(targetState = screenState, label = "header_text") { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (state) {
                            PinScreenState.ENTER_CURRENT_PIN -> "تطبيق المعلم محمي برمز PIN 🔒"
                            PinScreenState.RESET_NEW_PIN_STEP_1 -> "تعيين رمز PIN جديد للتطبيق 🔑"
                            PinScreenState.RESET_NEW_PIN_STEP_2 -> "تأكيد رمز PIN الجديد ✍️"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (state) {
                            PinScreenState.ENTER_CURRENT_PIN -> "أدخل رمز المرور السري (4 أرقام) للدخول"
                            PinScreenState.RESET_NEW_PIN_STEP_1 -> "أدخل رمز PIN الجديد المكون من 4 أرقام"
                            PinScreenState.RESET_NEW_PIN_STEP_2 -> "أعد إدخال نفس الرمز المكون من 4 أرقام للتأكيد"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pin Dots (4 dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) {
                                    if (screenState == PinScreenState.ENTER_CURRENT_PIN) NavyPrimary else Color(0xFFD97706)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad (1 to 9, 0, DEL)
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { digit ->
                            if (digit.isEmpty()) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else if (digit == "DEL") {
                                OutlinedButton(
                                    onClick = {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMessage = null
                                        }
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .testTag("pin_btn_del"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Backspace,
                                        contentDescription = "مسح",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (enteredPin.length < 4) {
                                            enteredPin += digit
                                            errorMessage = null
                                            if (enteredPin.length == 4) {
                                                when (screenState) {
                                                    PinScreenState.ENTER_CURRENT_PIN -> {
                                                        val success = PinLockManager.checkPin(enteredPin)
                                                        if (success) {
                                                            onUnlocked()
                                                        } else {
                                                            errorMessage = "رمز PIN غير صحيح! يمكنك استخدام قفل الهاتف إذا نسيت الرمز"
                                                            enteredPin = ""
                                                        }
                                                    }
                                                    PinScreenState.RESET_NEW_PIN_STEP_1 -> {
                                                        temporaryNewPin = enteredPin
                                                        enteredPin = ""
                                                        errorMessage = null
                                                        screenState = PinScreenState.RESET_NEW_PIN_STEP_2
                                                    }
                                                    PinScreenState.RESET_NEW_PIN_STEP_2 -> {
                                                        if (enteredPin == temporaryNewPin) {
                                                            PinLockManager.setPin(enteredPin)
                                                            Toast.makeText(context, "تم تغيير رمز PIN وتأمين التطبيق بنجاح ✅", Toast.LENGTH_SHORT).show()
                                                            onUnlocked()
                                                        } else {
                                                            errorMessage = "الرمزان غير متطابقين! أعد كتابة رمز PIN الجديد"
                                                            enteredPin = ""
                                                            temporaryNewPin = ""
                                                            screenState = PinScreenState.RESET_NEW_PIN_STEP_1
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .testTag("pin_btn_$digit"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = digit,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Forgot PIN / Phone Lock Authentication Action
            if (screenState == PinScreenState.ENTER_CURRENT_PIN) {
                OutlinedButton(
                    onClick = { requestDeviceCredentialUnlock() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("forgot_pin_device_auth_btn")
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "نسيت الرمز؟ الدخول بقفل الهاتف 📱",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "🔒 حماية صارمة: لا يمكن تخطي قفل التطبيق إلا بإدخال الرمز أو بقفل الهاتف أو مسح بيانات التطبيق",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                TextButton(
                    onClick = {
                        enteredPin = ""
                        temporaryNewPin = ""
                        errorMessage = null
                        screenState = PinScreenState.ENTER_CURRENT_PIN
                    }
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الرجوع لشاشة القفل الرئيسية")
                }
            }
        }
    }

    if (showNoDeviceSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showNoDeviceSecurityDialog = false },
            icon = { Icon(Icons.Filled.SecurityUpdateWarning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("قفل الهاتف غير مفعل", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "لم يتم العثور على قفل شاشة (PIN أو نمط أو كلمة مرور أو بصمة) مفعل على هذا الهاتف في إعدادات النظام.\n\nلتتمكن من إعادة تعيين الرمز، يرجى تفعيل قفل الشاشة في إعدادات هاتفك أولاً أو إدخال رمز التطبيق الصحيح."
                )
            },
            confirmButton = {
                Button(onClick = { showNoDeviceSecurityDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}
