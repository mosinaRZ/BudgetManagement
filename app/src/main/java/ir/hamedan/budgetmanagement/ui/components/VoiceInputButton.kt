package ir.hamedan.budgetmanagement.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * دکمه میکروفون برای ورود متن با ویس.
 * از SpeechRecognizer استفاده می‌کند (پشتیبانی فارسی در اکثر دستگاه‌های اندروید).
 *
 * @param onResult متن تشخیص‌داده‌شده به این callback پاس داده می‌شود
 * @param language کد زبان (پیش‌فرض fa-IR)
 * @param enabled آیا دکمه فعال باشد
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    language: String = "fa-IR",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // درخواست permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(
                context = context,
                language = language,
                onListeningChange = { isListening = it },
                onResult = onResult,
                onRecognizerCreated = { speechRecognizer = it }
            )
        } else {
            // اگر اندروید دیگر دیالوگ نشان نمی‌دهد (رد دائمی)، تنها راه فعال‌سازی تنظیمات برنامه است.
            val activity = context.findActivity()
            val permanentlyDenied = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            val isFa = language.startsWith("fa")
            Toast.makeText(
                context,
                when {
                    permanentlyDenied && isFa -> "برای ثبت با صدا، دسترسی میکروفون را از تنظیمات فعال کنید"
                    permanentlyDenied -> "Enable microphone access in Settings to use voice entry"
                    isFa -> "دسترسی به میکروفون لازم است"
                    else -> "Microphone permission required"
                },
                if (permanentlyDenied) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
            if (permanentlyDenied) context.openAppSettings()
        }
    }

    // پاک‌سازی هنگام خروج از composition
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        }
    }

    IconButton(
        onClick = {
            if (!enabled) return@IconButton

            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
                return@IconButton
            }

            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startListening(
                    context = context,
                    language = language,
                    onListeningChange = { isListening = it },
                    onResult = onResult,
                    onRecognizerCreated = { speechRecognizer = it }
                )
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = if (isListening) "Listening" else "Voice input",
            tint = if (isListening) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun startListening(
    context: android.content.Context,
    language: String,
    onListeningChange: (Boolean) -> Unit,
    onResult: (String) -> Unit,
    onRecognizerCreated: (SpeechRecognizer) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(
            context,
            if (language.startsWith("fa")) "تشخیص گفتار در این دستگاه پشتیبانی نمی‌شود"
            else "Speech recognition not available",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    onRecognizerCreated(recognizer)

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListeningChange(true)
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            onListeningChange(false)
        }

        override fun onError(error: Int) {
            onListeningChange(false)
            // خطاهای رایج را نادیده می‌گیریم تا UX خراب نشود
        }

        override fun onResults(results: Bundle?) {
            onListeningChange(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                onResult(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    try {
        recognizer.startListening(intent)
    } catch (e: Exception) {
        onListeningChange(false)
        Toast.makeText(context, "خطا در شروع تشخیص گفتار", Toast.LENGTH_SHORT).show()
    }
}