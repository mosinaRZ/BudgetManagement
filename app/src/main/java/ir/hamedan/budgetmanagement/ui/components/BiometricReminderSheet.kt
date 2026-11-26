package ir.hamedan.budgetmanagement.ui.components

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import ir.hamedan.budgetmanagement.data.preferences.BiometricReminderPreferences
import ir.hamedan.budgetmanagement.data.preferences.BiometricReminderTrigger
import ir.hamedan.budgetmanagement.data.preferences.NotificationType
import ir.hamedan.budgetmanagement.data.preferences.OnboardingPreferences
import ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
import ir.hamedan.budgetmanagement.data.security.RememberedLoginStore
import ir.hamedan.budgetmanagement.platform.biometric.BiometricPromptManager
import ir.hamedan.budgetmanagement.ui.screens.settings.EnableBiometricLoginDialog
import ir.hamedan.budgetmanagement.ui.screens.settings.enrollGuestBiometric
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the home screen is left alone before the reminder may appear after getting in. */
private const val SETTLE_DELAY_MS = 1_500L

/** Pause after the app returns to the foreground, so a lock screen can take over first. */
private const val RETURN_SETTLE_DELAY_MS = 1_500L

/** How often the host re-checks whether onboarding / another sheet is still in the way. */
private const val READY_POLL_MS = 500L

/**
 * Hosts the "sign in faster with your fingerprint" reminder.
 *
 * It is deliberately persistent (see [BiometricReminderPolicy][ir.hamedan.budgetmanagement.data.preferences.BiometricReminderPolicy]):
 *  - every time the user gets into the app (sign-in, local unlock, cold start) the reminder is
 *    shown, as long as fingerprint login is still off;
 *  - while the user stays in the app, coming back from the background shows it again after a
 *    short interval;
 *  - there is no "don't ask again": the only way to stop it is to enable fingerprint login.
 *
 * "Enable" runs the same enrollment as Settings > App Security right there, so enabling takes one
 * tap instead of a trip through Settings. "Later" just closes the sheet for now.
 *
 * Nothing is shown when the device has no enrolled strong biometric or when fingerprint login is
 * already on. A due reminder waits (instead of being skipped) while the onboarding or the
 * permissions sheet is open and appears as soon as they are gone.
 */
@Composable
fun BiometricReminderHost(
    isPersian: Boolean,
    blockedByOtherSheet: Boolean
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as BudgetApp }
    val blocked by rememberUpdatedState(blockedByOtherSheet)
    val scope = rememberCoroutineScope()

    // Saveable: a rotation must neither lose an open sheet nor count as a new sign-in.
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var entryHandled by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }
    val checking = remember { AtomicBoolean(false) }

    /** Shows the reminder if it is due for [trigger] and fingerprint login can be offered. */
    suspend fun remindIfDue(trigger: BiometricReminderTrigger) {
        if (showSheet || showPasswordDialog || !checking.compareAndSet(false, true)) return
        try {
            // Wait for the first-run onboarding and any permission sheet instead of skipping.
            while (!OnboardingPreferences.isCompleted(context) || blocked) delay(READY_POLL_MS)
            if (showSheet || showPasswordDialog) return
            if (!canOfferFingerprint(context, app)) return
            if (!BiometricReminderPreferences.shouldRemindNow(context, trigger)) return
            BiometricReminderPreferences.markShownNow(context)
            showSheet = true
        } finally {
            checking.set(false)
        }
    }

    // Getting into the app (a fresh sign-in, a local unlock or a cold start) always reminds.
    LaunchedEffect(Unit) {
        if (entryHandled) return@LaunchedEffect
        delay(SETTLE_DELAY_MS)
        remindIfDue(BiometricReminderTrigger.SESSION_ENTRY)
        entryHandled = true
    }

    // Coming back to the app from the background reminds again after a short interval.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var registered = false
        val observer = LifecycleEventObserver { _, event ->
            // Adding an observer replays the current state; only real returns count.
            if (registered && event == Lifecycle.Event.ON_START) {
                scope.launch {
                    delay(RETURN_SETTLE_DELAY_MS)
                    remindIfDue(BiometricReminderTrigger.RETURN_TO_APP)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        registered = true
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onEnabled() {
        showSheet = false
        showPasswordDialog = false
        setupError = null
        NotificationHelper.send(
            context = context,
            notificationType = NotificationType.SETTINGS_CHANGED,
            type = "SYSTEM",
            titleFa = "ورود با اثر انگشت فعال شد",
            titleEn = "Biometric login enabled",
            descFa = "از این پس می\u200cتوانید بدون وارد کردن رمز عبور وارد شوید.",
            descEn = "You can now unlock the app without typing your password.",
            tag = "SETTINGS_CHANGED_${System.currentTimeMillis()}"
        )
    }

    fun unavailableMessage() =
        if (isPersian) "بیومتریک دستگاه آماده استفاده نیست." else "Biometric authentication is not available on this device."

    fun startEnable() {
        setupError = null
        val activity = context as? FragmentActivity
        if (activity == null) {
            setupError = unavailableMessage()
            return
        }
        val guestStore = app.container.guestModeStore
        if (guestStore.isSessionActive) {
            enrollGuestBiometric(activity, guestStore, isPersian) { enabled, error ->
                if (enabled) {
                    onEnabled()
                } else {
                    setupError = error
                }
            }
            return
        }
        val store = app.container.rememberedLoginStore
        if (store.hasBiometricCredential() && store.canUseStrongBiometric()) {
            // The secure credential already exists: confirming the fingerprint is enough.
            BiometricPromptManager.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    SharedPreferences.setBiometricEnabled(context, true)
                    onEnabled()
                }
            )
        } else {
            showPasswordDialog = true
        }
    }

    if (showSheet) {
        BiometricReminderSheet(
            isPersian = isPersian,
            errorMessage = setupError,
            onEnable = { startEnable() },
            onLater = {
                showSheet = false
                setupError = null
            }
        )
    }

    if (showPasswordDialog) {
        EnableBiometricLoginDialog(
            isPersian = isPersian,
            errorMessage = setupError,
            onDismiss = {
                showPasswordDialog = false
                setupError = null
            },
            onConfirm = { currentPassword ->
                val activity = context as? FragmentActivity
                val store = app.container.rememberedLoginStore
                if (!store.verifyPassword(currentPassword)) {
                    setupError = if (isPersian) "رمز عبور فعلی نادرست است." else "The current password is incorrect."
                } else if (activity == null) {
                    setupError = unavailableMessage()
                } else {
                    enrollAccountBiometric(activity, store, isPersian) { enabled, error ->
                        if (enabled) {
                            SharedPreferences.setBiometricEnabled(context, true)
                            onEnabled()
                        } else {
                            setupError = error
                        }
                    }
                }
            }
        )
    }
}

/** True when the device can use fingerprint login here and it is not enabled yet. */
private fun canOfferFingerprint(context: Context, app: BudgetApp): Boolean {
    val strong = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    // No sensor, nothing enrolled, or temporarily unavailable: do not advertise it.
    if (strong != BiometricManager.BIOMETRIC_SUCCESS) return false

    val guestStore = app.container.guestModeStore
    return if (guestStore.isSessionActive) {
        // Guest fingerprint falls back to the guest passcode, which must exist first.
        guestStore.hasPasscode() && !guestStore.isBiometricEnabled()
    } else {
        val store = app.container.rememberedLoginStore
        !SharedPreferences.getBiometricEnabled(context) &&
                store.canUseStrongBiometric() &&
                // Enabling asks for the current password once; it must be possible to check it.
                store.hasPasswordVerifier()
    }
}

/** Creates the Keystore-backed credential for the signed-in account after a fingerprint check. */
private fun enrollAccountBiometric(
    activity: FragmentActivity,
    store: RememberedLoginStore,
    isPersian: Boolean,
    onResult: (Boolean, String?) -> Unit
) {
    val cipher = store.prepareEncryptionCipher()
    if (cipher == null) {
        onResult(false, if (isPersian) "بیومتریک دستگاه آماده استفاده نیست." else "Biometric authentication is not available on this device.")
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authenticated = result.cryptoObject?.cipher
                if (authenticated == null) {
                    onResult(false, if (isPersian) "احراز هویت بیومتریک کامل نشد." else "Biometric authentication could not be completed.")
                    return
                }
                store.enrollBiometric(authenticated)
                val enabled = store.hasBiometricCredential()
                onResult(
                    enabled,
                    if (enabled) null
                    else if (isPersian) "فعال\u200cسازی ورود با اثر انگشت انجام نشد." else "Biometric login could not be enabled."
                )
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val userStopped = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                onResult(false, if (userStopped) null else errString.toString().takeIf { it.isNotBlank() })
            }
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isPersian) "فعال\u200cسازی ورود با اثر انگشت" else "Enable biometric login")
            .setSubtitle(if (isPersian) "برای فعال\u200cسازی، اثر انگشت خود را تأیید کنید" else "Confirm your fingerprint to enable login")
            .setNegativeButtonText(if (isPersian) "انصراف" else "Cancel")
            .build(),
        BiometricPrompt.CryptoObject(cipher)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BiometricReminderSheet(
    isPersian: Boolean,
    errorMessage: String?,
    onEnable: () -> Unit,
    onLater: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isPersian) "ورود سریع\u200cتر با اثر انگشت" else "Sign in faster with your fingerprint",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPersian) "دیگر لازم نیست هر بار رمز عبور را تایپ کنید؛ با یک لمس وارد برنامه می\u200cشوید."
                else "No more typing your password every time: unlock the app with a single touch.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BenefitRow(
                    icon = Icons.Filled.Fingerprint,
                    title = if (isPersian) "ورود با یک لمس" else "One-touch sign-in",
                    body = if (isPersian) "بدون تایپ رمز، در کمتر از یک ثانیه وارد می\u200cشوید." else "No typing: you are in within a second."
                )
                BenefitRow(
                    icon = Icons.Rounded.Lock,
                    title = if (isPersian) "امن و فقط روی همین گوشی" else "Secure, on this phone only",
                    body = if (isPersian) "کلید ورود با قفل امن اندروید (Keystore) محافظت می\u200cشود و فقط با اثر انگشت شما باز می\u200cشود."
                    else "The sign-in key is protected by Android Keystore and only opens with your fingerprint."
                )
                BenefitRow(
                    icon = Icons.Rounded.Settings,
                    title = if (isPersian) "هر زمان خواستید خاموشش کنید" else "Turn it off anytime",
                    body = if (isPersian) "از تنظیمات ← امنیت برنامه ← ورود با اثر انگشت."
                    else "Settings → App Security → Biometric Login."
                )
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = colors.error
                )
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isPersian) "فعال\u200cسازی اثر انگشت" else "Enable fingerprint",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(6.dp))

            TextButton(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isPersian) "فعلاً نه، بعداً" else "Not now",
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}