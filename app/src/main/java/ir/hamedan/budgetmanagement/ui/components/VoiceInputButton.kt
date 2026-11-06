package ir.hamedan.budgetmanagement.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * دکمه میکروفون برای ورود متن با ویس.
 *
 * از `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` استفاده می‌کند؛ یعنی رابط تشخیص گفتار سیستم (معمولاً گوگل)
 * خودش میکروفون را مدیریت می‌کند و خود برنامه به مجوز `RECORD_AUDIO` نیاز ندارد.
 *
 * @param onResult متن تشخیص‌داده‌شده به این callback پاس داده می‌شود
 * @param language کد زبان (پیش‌فرض fa-IR)
 * @param enabled آیا دکمه فعال باشد
 */
/**
 * راه‌انداز تشخیص گفتار برای جاهایی که کل یک ردیف باید قابل‌لمس باشد، نه فقط آیکن میکروفون.
 * خروجی تابعی است که با صدا زدنش رابط تشخیص گفتار سیستم باز می‌شود.
 */
@Composable
fun rememberVoiceInputLauncher(
    onResult: (String) -> Unit,
    language: String = "fa-IR",
    enabled: Boolean = true
): () -> Unit {
    val context = LocalContext.current
    val isFa = language.startsWith("fa")
    val latestOnResult by rememberUpdatedState(onResult)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // انصراف کاربر / نبودن گفتار = نتیجهٔ عادی؛ بدون پیام مزاحم.
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrBlank()) latestOnResult(text)
        }
    }

    return remember(enabled, language, speechLauncher) {
        launch@{
            if (!enabled) return@launch

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    if (isFa) "تشخیص گفتار در این دستگاه پشتیبانی نمی‌شود" else "Speech recognition not available",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    if (isFa) "شروع تشخیص گفتار ناموفق بود" else "Could not start speech recognition",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    language: String = "fa-IR",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val launchSpeech = rememberVoiceInputLauncher(onResult, language, enabled)

    IconButton(
        onClick = launchSpeech,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.MicNone,
            contentDescription = "Voice input",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}