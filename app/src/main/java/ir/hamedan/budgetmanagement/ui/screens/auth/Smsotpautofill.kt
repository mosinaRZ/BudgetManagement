package ir.hamedan.budgetmanagement.ui.screens.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Listens for the verification SMS with Google's SMS User Consent API.
 *
 * - No SMS permission is needed in the Manifest. Google Play Services shows a system dialog and
 *   only hands the app the one message the user approves.
 * - Listening lasts up to 5 minutes after [listenKey] changes (a Play Services limit).
 * - If Google Play Services is missing (some Bazaar/Myket devices), the request just fails
 *   silently and the user types the code or uses the keyboard suggestion instead.
 *
 * @param listenKey pass null to stay idle; pass a new value (e.g. the time an OTP was requested)
 *   to start a fresh listening session.
 */
@Composable
fun SmsUserConsentEffect(
    listenKey: Any?,
    onCodeReceived: (String) -> Unit
) {
    val context = LocalContext.current
    val currentOnCode by rememberUpdatedState(onCodeReceived)

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            SmsOtpParser.extractCode(message)?.let(currentOnCode)
        }
    }

    DisposableEffect(listenKey) {
        if (listenKey == null) return@DisposableEffect onDispose { }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val extras = intent.extras ?: return
                val status = BundleCompat.getParcelable(extras, SmsRetriever.EXTRA_STATUS, Status::class.java)
                    ?: return
                if (status.statusCode == CommonStatusCodes.SUCCESS) {
                    val consentIntent = BundleCompat.getParcelable(
                        extras,
                        SmsRetriever.EXTRA_CONSENT_INTENT,
                        Intent::class.java
                    )
                    if (consentIntent != null) {
                        try {
                            consentLauncher.launch(consentIntent)
                        } catch (_: ActivityNotFoundException) {
                            // The user types the code manually.
                        }
                    }
                }
                // CommonStatusCodes.TIMEOUT: nothing arrived within 5 minutes; nothing to do.
            }
        }

        // Exported on purpose: the broadcast comes from Play Services. SEND_PERMISSION makes sure
        // only Play Services can deliver it.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )

        // null sender = accept the first suitable SMS from any sender.
        runCatching {
            SmsRetriever.getClient(context).startSmsUserConsent(null)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered.
            }
        }
    }
}

object SmsOtpParser {
    private val sixDigits = Regex("(?<!\\d)\\d{6}(?!\\d)")

    /** Finds the first stand-alone 6-digit code. Persian and Arabic-Indic digits are converted first. */
    fun extractCode(message: String?): String? {
        if (message.isNullOrBlank()) return null
        val ascii = buildString(message.length) {
            for (ch in message) {
                append(
                    when (ch) {
                        in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                        in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                        else -> ch
                    }
                )
            }
        }
        return sixDigits.find(ascii)?.value
    }
}

/**
 * Marks a field as an SMS one-time code, so keyboards and autofill services can offer the code
 * they have seen in the incoming SMS. Apply it to the OTP field's modifier.
 * Needs Compose UI 1.8+ (BOM 2025.04 or newer).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.smsOtpContentType(): Modifier = semantics { contentType = ContentType.SmsOtpCode }