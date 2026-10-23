package ir.hamedan.budgetmanagement.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import ir.hamedan.budgetmanagement.data.network.ApiException
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hamedan.budgetmanagement.ui.components.AppLogoIcon
import ir.hamedan.budgetmanagement.data.guest.GuestModeManager
import ir.hamedan.budgetmanagement.data.security.AuthSessionStore
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.preferences.SharedPreferences
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.data.notification.NotificationHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    localUnlockOnly: Boolean = false
) {
    val context = LocalContext.current
    val isPersian = isPersianLocale()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as BudgetApp
    val authRepository = remember { app.container.authRepository }
    val rememberedLoginStore = remember { app.container.rememberedLoginStore }
    val guestStore = remember { app.container.guestModeStore }
    val guestManager = remember { app.container.guestModeManager }

    var username by remember { mutableStateOf(rememberedLoginStore.identifier().orEmpty()) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var biometricPromptShown by remember(localUnlockOnly) { mutableStateOf(false) }
    var biometricCanceled by remember(localUnlockOnly) { mutableStateOf(false) }

    // Guest state. The guest workspace never talks to the server, so none of these touch the network.
    var guestPasscode by remember { mutableStateOf("") }
    var guestPasscodeRequested by remember { mutableStateOf(false) }
    var guestBiometricPromptShown by remember { mutableStateOf(false) }
    var showGuestDataChoice by remember { mutableStateOf(false) }
    var pendingAccountIdentifier by remember { mutableStateOf<String?>(null) }
    var pendingDiscardGuestData by remember { mutableStateOf(false) }

    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // The workspace is open in memory and this screen is only locking it again (not signing in).
    val guestLock = localUnlockOnly && guestStore.isSessionActive

    // A guest workspace with a passcode or fingerprint is on this device and the app was opened
    // again: say that the guest is entering and ask for that secret, instead of the account form.
    // The account form stays one tap away.
    var accountFormRequested by remember { mutableStateOf(false) }
    val guestPrompt = !localUnlockOnly && !guestStore.isSessionActive && !accountFormRequested &&
            guestStore.hasWorkspace() && guestStore.hasUnlockSecret()

    // The account whose session is still on this device (the one the local unlock belongs to).
    val rememberedIdentifier = remember { rememberedLoginStore.identifier().orEmpty() }
    // Set once the user chose to sign in to a different account: the form then acts as a full sign-in.
    var accountSwitchRequested by remember(localUnlockOnly) { mutableStateOf(false) }
    val unlockMode = localUnlockOnly && !accountSwitchRequested && !guestStore.isSessionActive

    // The server ended this session (expired, or this device was removed from the account):
    // tell the user why they are back here instead of showing an unexplained login form.
    LaunchedEffect(localUnlockOnly) {
        if (!localUnlockOnly) {
            errorMessage = when (app.container.authSessionStore.consumeEnd()) {
                AuthSessionStore.SessionEnd.DEVICE_REMOVED -> if (isPersian) {
                    "این دستگاه از حساب شما حذف شده است. برای استفاده دوباره، با رمز عبور وارد شوید. اگر این کار را شما انجام نداده‌اید، پس از ورود رمز عبور خود را تغییر دهید."
                } else {
                    "This device was removed from your account. Sign in with your password to use it again. If you didn't do this, change your password after signing in."
                }
                AuthSessionStore.SessionEnd.EXPIRED -> if (isPersian) {
                    "نشست شما پایان یافته است. برای ادامه دوباره وارد شوید."
                } else {
                    "Your session has ended. Please sign in again."
                }
                AuthSessionStore.SessionEnd.NONE -> errorMessage
            }
        }
    }

    val biometricLoginEnabled = remember(context) { SharedPreferences.getBiometricEnabled(context) }
    val biometricReady = biometricLoginEnabled && rememberedLoginStore.hasBiometricCredential() && rememberedLoginStore.canUseStrongBiometric()
    val passwordUnlockReady = rememberedLoginStore.hasPasswordVerifier()

    // ---------------------------------------------------------------- account (server) helpers

    fun finishLocalUnlock() {
        isLoggingIn = false
        password = ""
        onLoginSuccess()
    }

    /**
     * True when the identifier typed in unlock mode is not the account this device is unlocking.
     * The remembered password and fingerprint belong to that account, so they must never unlock
     * the app for a different identifier.
     */
    fun typedAccountDiffers(): Boolean {
        if (!unlockMode || rememberedIdentifier.isBlank()) return false
        val typed = AuthInputNormalizer.identifier(username) ?: username.trim()
        return !typed.equals(rememberedIdentifier, ignoreCase = true)
    }

    /**
     * Signs the current account out of this device (after its unsynced changes are synced; refused
     * when that is not possible), so the user can sign in to the other account with its own password.
     */
    fun switchAccount() {
        if (isLoggingIn) return
        isLoggingIn = true
        errorMessage = null
        scope.launch {
            val ready = authRepository.prepareAccountSwitch().getOrDefault(false)
            isLoggingIn = false
            if (ready) {
                accountSwitchRequested = true
            } else {
                errorMessage = if (isPersian) {
                    "برای ورود به حساب دیگر، ابتدا تغییرات حساب فعلی باید همگام‌سازی شود. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید."
                } else {
                    "Before switching accounts, the current account's changes must sync. Check your connection and try again."
                }
            }
        }
    }

    fun startBiometricUnlock() {
        if (isLoggingIn || biometricPromptShown) return
        if (typedAccountDiffers()) {
            errorMessage = if (isPersian) "اثر انگشت فقط برای حساب ذخیره‌شده روی این دستگاه است. برای حساب دیگر، رمز عبورش را وارد کنید." else "Fingerprint only unlocks the account saved on this device. For another account, enter its password."
            return
        }
        val activity = context as? FragmentActivity
        val cipher = rememberedLoginStore.prepareDecryptionCipher()
        if (activity == null || cipher == null) {
            errorMessage = if (isPersian) "ورود بیومتریک آماده نیست؛ لطفاً با رمز عبور ادامه دهید." else "Biometric login is unavailable; continue with your password."
            return
        }

        biometricPromptShown = true
        isLoggingIn = true
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Local unlock only. No access/refresh token is touched and no network request is made.
                // The hardware key must really have been released, not merely the prompt dismissed.
                if (result.cryptoObject?.cipher?.let { rememberedLoginStore.confirmBiometric(it) } == true) {
                    finishLocalUnlock()
                } else {
                    isLoggingIn = false
                    biometricPromptShown = false
                    errorMessage = if (isPersian) "ورود بیومتریک انجام نشد؛ با رمز عبور ادامه دهید." else "Biometric login failed; continue with your password."
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isLoggingIn = false
                biometricPromptShown = false
                errorMessage = null
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                errorMessage = if (isPersian) "اثر انگشت شناسایی نشد؛ دوباره تلاش کنید." else "Biometric not recognized; try again."
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isPersian) "تأیید هویت برای ورود" else "Verify to continue")
                .setSubtitle(if (isPersian) "برای ورود به برنامه اثر انگشت خود را تأیید کنید" else "Confirm your biometric to unlock the app")
                .setNegativeButtonText(if (isPersian) "انصراف" else "Cancel")
                .build(),
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    fun submitLocalPassword() {
        if (isLoggingIn) return
        if (typedAccountDiffers()) {
            switchAccount()
            return
        }
        if (!passwordUnlockReady) {
            errorMessage = if (isPersian) "رمز ورود محلی هنوز آماده نیست؛ یک‌بار با اتصال به سرور وارد شوید." else "Local password verification is not initialized; sign in once with the server."
            return
        }
        isLoggingIn = true
        errorMessage = null
        if (rememberedLoginStore.verifyPassword(password)) {
            finishLocalUnlock()
        } else {
            isLoggingIn = false
            errorMessage = if (isPersian) "رمز عبور نادرست است." else "Incorrect password."
        }
    }

    /**
     * Signs in against the server. [restoreAccount] is only ever true after the server said the
     * account was deleted and the user explicitly agreed to recover it in the dialog below.
     * [discardGuestData] is applied by the repository only after the credentials were verified.
     */
    fun runServerLogin(identifier: String, restoreAccount: Boolean, discardGuestData: Boolean) {
        isLoggingIn = true
        errorMessage = null
        scope.launch {
            authRepository.login(identifier, password, restoreAccount, discardGuestData)
                .onSuccess {
                    runCatching {
                        app.seedDefaultCategoriesIfNeeded()
                        rememberedLoginStore.saveIdentifier(identifier)
                        rememberedLoginStore.savePasswordVerifier(password)
                        NotificationHelper.sendWelcomeIfNeeded(context)

                        // Biometric enrollment is opt-in and is performed only from Settings.
                        isLoggingIn = false
                        password = ""
                        onLoginSuccess()
                    }.onFailure { error ->
                        isLoggingIn = false
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: if (isPersian) "آماده‌سازی اولیه برنامه انجام نشد" else "Initial app setup failed"
                    }
                }
                .onFailure { error ->
                    isLoggingIn = false
                    if ((error as? ApiException)?.code == "ACCOUNT_DELETED") {
                        // Correct credentials, but the account was deleted: offer to restore it.
                        pendingDiscardGuestData = discardGuestData
                        showRestoreDialog = true
                    } else {
                        errorMessage = authErrorMessage(error, isPersian, AuthContext.LOGIN)
                    }
                }
        }
    }

    fun submitServerLogin() {
        if (isLoggingIn) return
        val validationError = LoginInputValidator.validate(username, password, isPersian)
        if (validationError != null) {
            errorMessage = validationError
            return
        }
        // "09123456789" / "0912 345 6789" / Persian digits -> "+989123456789" (what the API expects).
        val identifier = AuthInputNormalizer.identifier(username)
        if (identifier == null) {
            errorMessage = if (isPersian) "شماره موبایل معتبر نیست" else "Enter a valid phone number"
            return
        }
        // A guest workspace on this device must be decided on before it can merge into an account.
        if (guestStore.hasWorkspace()) {
            // The account was just created from this device: its choice was made on the register screen.
            val decided = guestStore.pendingRegistrationDiscard(identifier)
            if (decided != null) {
                runServerLogin(identifier, restoreAccount = false, discardGuestData = decided)
                return
            }
            pendingAccountIdentifier = identifier
            showGuestDataChoice = true
            return
        }
        runServerLogin(identifier, restoreAccount = false, discardGuestData = false)
    }

    // ---------------------------------------------------------------- guest helpers

    /** Opens the guest workspace (no network). Called only after any required guest unlock succeeded. */
    fun openGuestWorkspace() {
        if (isLoggingIn) return
        isLoggingIn = true
        errorMessage = null
        scope.launch {
            guestManager.startSession()
                .onSuccess {
                    // Seeding happens after the session started, so it lands in the workspace that is open.
                    runCatching { app.seedDefaultCategoriesIfNeeded() }
                    guestPasscode = ""
                    guestPasscodeRequested = false
                    isLoggingIn = false
                    onLoginSuccess()
                }
                .onFailure { error ->
                    isLoggingIn = false
                    errorMessage = if (error is GuestModeManager.UnsyncedAccountException) {
                        if (isPersian) {
                            "روی این دستگاه داده‌ای از یک حساب هست که هنوز همگام‌سازی نشده. ابتدا با همان حساب وارد شوید تا همگام شود."
                        } else {
                            "This device holds data of an account that has not synced yet. Sign in to that account first so it can sync."
                        }
                    } else {
                        if (isPersian) "ورود مهمان انجام نشد. دوباره تلاش کنید." else "Guest sign-in failed. Please try again."
                    }
                }
        }
    }

    /** Ends the lock of an already open guest workspace. */
    fun completeGuestUnlock() {
        isLoggingIn = false
        guestPasscode = ""
        onLoginSuccess()
    }

    /**
     * A verified guest secret: unlocks an open workspace, or starts the workspace when the guest
     * prompt was shown at app start (no workspace session exists yet in that case).
     */
    fun onGuestVerified() {
        if (guestLock) completeGuestUnlock() else openGuestWorkspace()
    }

    /**
     * Fingerprint for the guest. [onVerified] runs on the main thread after the Keystore cipher
     * proved the fingerprint. If the key was invalidated, fingerprint unlock is switched off and the
     * guest is sent to the passcode.
     */
    fun startGuestBiometric(onVerified: () -> Unit) {
        if (isLoggingIn) return
        val activity = context as? FragmentActivity
        val cipher = guestStore.prepareUnlockCipher()
        if (activity == null || cipher == null) {
            errorMessage = if (isPersian) {
                "اثر انگشت برای حالت مهمان در دسترس نیست. با گذرواژه مهمان ادامه دهید."
            } else {
                "Fingerprint is unavailable for guest mode. Continue with the guest passcode."
            }
            if (guestStore.hasPasscode()) guestPasscodeRequested = true
            return
        }

        guestBiometricPromptShown = true
        isLoggingIn = true
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val verified = result.cryptoObject?.cipher?.let { guestStore.completeBiometricUnlock(it) } == true
                    guestBiometricPromptShown = false
                    isLoggingIn = false
                    if (verified) {
                        onVerified()
                    } else {
                        errorMessage = if (isPersian) "احراز هویت با اثر انگشت کامل نشد." else "Fingerprint verification did not complete."
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    isLoggingIn = false
                    guestBiometricPromptShown = false
                    val userStopped = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                    if (!userStopped) errorMessage = errString.toString().takeIf { it.isNotBlank() }
                }

                override fun onAuthenticationFailed() {
                    errorMessage = if (isPersian) "اثر انگشت شناسایی نشد؛ دوباره تلاش کنید." else "Biometric not recognized; try again."
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isPersian) "ورود به حالت مهمان" else "Guest sign-in")
                .setSubtitle(if (isPersian) "برای ورود، اثر انگشت خود را تأیید کنید" else "Confirm your fingerprint to continue")
                .setNegativeButtonText(if (isPersian) "انصراف" else "Cancel")
                .build(),
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    /** Guest button on the sign-in screen: the method depends on what the guest set up on this device. */
    fun onGuestEntryClick() {
        if (isLoggingIn) return
        errorMessage = null
        val hasWorkspace = guestStore.hasWorkspace()
        when {
            hasWorkspace && guestStore.isBiometricEnabled() -> startGuestBiometric(onVerified = ::openGuestWorkspace)
            hasWorkspace && guestStore.hasPasscode() -> guestPasscodeRequested = true
            else -> openGuestWorkspace()
        }
    }

    fun submitGuestPasscode() {
        if (isLoggingIn) return
        if (guestPasscode.isEmpty()) {
            errorMessage = if (isPersian) "گذرواژه مهمان را وارد کنید." else "Enter the guest passcode."
            return
        }
        if (!guestStore.verifyPasscode(guestPasscode)) {
            errorMessage = if (isPersian) "گذرواژه مهمان نادرست است." else "Incorrect guest passcode."
            return
        }
        if (guestLock) completeGuestUnlock() else openGuestWorkspace()
    }

    // Lock of an open guest workspace, or the guest prompt at app start: ask for the fingerprint
    // straight away, once.
    LaunchedEffect(guestLock, guestPrompt) {
        if ((guestLock || guestPrompt) && guestStore.isBiometricEnabled() && !guestBiometricPromptShown) {
            startGuestBiometric(onVerified = ::onGuestVerified)
        }
    }

    // ---------------------------------------------------------------- dialogs

    if (showGuestDataChoice) {
        GuestDataChoiceDialog(
            isPersian = isPersian,
            onMoveToAccount = {
                showGuestDataChoice = false
                pendingAccountIdentifier?.let { runServerLogin(it, restoreAccount = false, discardGuestData = false) }
            },
            onDeleteGuestData = {
                showGuestDataChoice = false
                pendingAccountIdentifier?.let { runServerLogin(it, restoreAccount = false, discardGuestData = true) }
            },
            onDismiss = { showGuestDataChoice = false }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (isPersian) "این حساب حذف شده است" else "This account was deleted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isPersian)
                        "این حساب را قبلاً حذف کرده‌اید، اما هنوز در مهلت بازیابی هستید. با بازیابی، حساب و اطلاعات همگام‌شده‌ی شما دوباره فعال می‌شود."
                    else
                        "You deleted this account earlier, but it can still be restored. Restoring brings back your account and synced data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        val identifier = AuthInputNormalizer.identifier(username)
                        if (identifier != null) runServerLogin(identifier, restoreAccount = true, discardGuestData = pendingDiscardGuestData)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (isPersian) "بازیابی حساب" else "Restore account") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel")
                }
            }
        )
    }

    LaunchedEffect(unlockMode, biometricReady) {
        if (unlockMode && biometricReady) {
            startBiometricUnlock()
        }
    }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        val lastBackPressTime = LocalLoginBackPress.current
        if (currentTime - lastBackPressTime < 2500) {
            (context as? Activity)?.finish()
        } else {
            LocalLoginBackPress.current = currentTime
            Toast.makeText(context, if (isPersian) "برای خروج، دوباره دکمه بازگشت را بزنید" else "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------- UI

    /** From the guest prompt to the account form, for signing in to an account instead. */
    val onAccountSignIn: () -> Unit = {
        accountFormRequested = true
        guestPasscodeRequested = false
        guestPasscode = ""
        errorMessage = null
    }

    val guestUsesFingerprint = guestStore.hasWorkspace() && guestStore.isBiometricEnabled()
    val guestEntryLabel = when {
        guestUsesFingerprint -> if (isPersian) "ورود مهمان با اثر انگشت" else "Guest sign-in with fingerprint"
        guestStore.hasWorkspace() && guestStore.hasPasscode() -> if (isPersian) "ورود مهمان با گذرواژه" else "Guest sign-in with passcode"
        else -> if (isPersian) "ورود به عنوان مهمان" else "Continue as guest"
    }

    AuthScreenContainer {
        AuthHeader(
            icon = AppLogoIcon,
            title = if (isPersian) "خوش آمدید" else "Welcome back",
            subtitle = when {
                guestLock -> if (isPersian) "برای ادامه در حالت مهمان، هویت خود را تأیید کنید" else "Verify your identity to continue as guest"
                guestPrompt -> if (isPersian) "شما به عنوان مهمان وارد می‌شوید. برای ادامه، گذرواژه مهمان یا اثر انگشت خود را وارد کنید" else "You are entering as a guest. Enter your guest passcode or fingerprint to continue."
                unlockMode -> if (isPersian) "برای ادامه، هویت خود را تأیید کنید" else "Verify your identity to continue"
                guestPasscodeRequested -> if (isPersian) "برای ورود به حالت مهمان، گذرواژه مهمان را وارد کنید" else "Enter your guest passcode to continue as guest"
                else -> if (isPersian) "برای ورود، مشخصات حساب خود را وارد کنید" else "Sign in with your account details"
            }
        )

        if (guestLock || guestPrompt) {
            GuestLockCard(
                isPersian = isPersian,
                isLoading = isLoggingIn,
                errorMessage = errorMessage,
                fingerprintEnabled = guestStore.isBiometricEnabled(),
                passcodeEnabled = guestStore.hasPasscode(),
                showPasscode = guestPasscodeRequested || !guestStore.isBiometricEnabled(),
                passcode = guestPasscode,
                onPasscodeChange = { guestPasscode = it; errorMessage = null },
                onFingerprintClick = { startGuestBiometric(onVerified = ::onGuestVerified) },
                onUsePasscodeClick = { guestPasscodeRequested = true; errorMessage = null },
                onPasscodeSubmit = ::submitGuestPasscode,
                onContinueClick = ::onGuestVerified,
                onAccountSignInClick = if (guestPrompt) onAccountSignIn else null
            )
        } else if (guestPasscodeRequested) {
            GuestPasscodeCard(
                isPersian = isPersian,
                isLoading = isLoggingIn,
                errorMessage = errorMessage,
                passcode = guestPasscode,
                fingerprintEnabled = guestStore.isBiometricEnabled(),
                onPasscodeChange = { guestPasscode = it; errorMessage = null },
                onSubmit = ::submitGuestPasscode,
                onFingerprintClick = { startGuestBiometric(onVerified = ::openGuestWorkspace) },
                onBack = {
                    guestPasscodeRequested = false
                    guestPasscode = ""
                    errorMessage = null
                }
            )
        } else {
            AuthCard {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    enabled = !isLoggingIn,
                    label = { Text(if (isPersian) "شماره موبایل" else "Phone number") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next, hintLocales = LocaleList(Locale("en"))),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    enabled = !isLoggingIn,
                    label = { Text(if (isPersian) "گذرواژه" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (unlockMode) submitLocalPassword() else submitServerLogin() }),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPersian) "نمایش یا پنهان کردن گذرواژه" else "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester)
                )

                AnimatedVisibility(errorMessage != null) {
                    errorMessage?.let { AuthErrorBanner(message = it) }
                }

                LoadingButton(
                    text = when {
                        !unlockMode -> if (isPersian) "ورود به حساب" else "Sign In"
                        password.isBlank() && biometricReady -> if (isPersian) "ورود با اثر انگشت" else "Sign in with fingerprint"
                        else -> if (isPersian) "ورود با رمز عبور" else "Sign in with password"
                    },
                    isLoading = isLoggingIn,
                    loadingText = if (unlockMode) (if (isPersian) "در حال بررسی..." else "Verifying...") else null,
                    onClick = when {
                        !unlockMode -> ::submitServerLogin
                        password.isBlank() && biometricReady -> ::startBiometricUnlock
                        else -> ::submitLocalPassword
                    }
                )

                TextButton(
                    onClick = onForgotPassword,
                    enabled = !isLoggingIn,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isPersian) "رمز عبور را فراموش کرده‌اید؟" else "Forgot your password?",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!unlockMode) {
                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isPersian) "حساب کاربری ندارید؟" else "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(6.dp))
                        TextButton(
                            onClick = onRegister,
                            enabled = !isLoggingIn,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                if (isPersian) "ساخت حساب" else "Create account",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                GuestEntryButton(
                    label = guestEntryLabel,
                    usesFingerprint = guestUsesFingerprint,
                    enabled = !isLoggingIn,
                    onClick = ::onGuestEntryClick
                )
            }
        }
    }
}

/** Guest passcode form, shown in place of the account form when the guest chose the passcode. */
@Composable
private fun GuestPasscodeCard(
    isPersian: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    passcode: String,
    fingerprintEnabled: Boolean,
    onPasscodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFingerprintClick: () -> Unit,
    onBack: () -> Unit
) {
    AuthCard {
        OutlinedTextField(
            value = passcode,
            onValueChange = onPasscodeChange,
            enabled = !isLoading,
            label = { Text(if (isPersian) "گذرواژه مهمان" else "Guest passcode") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = credentialTextStyle(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(errorMessage != null) {
            errorMessage?.let { AuthErrorBanner(message = it) }
        }

        LoadingButton(
            text = if (isPersian) "ورود مهمان" else "Continue as guest",
            isLoading = isLoading,
            onClick = onSubmit
        )

        if (fingerprintEnabled) {
            TextButton(
                onClick = onFingerprintClick,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isPersian) "ورود با اثر انگشت" else "Use fingerprint instead",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        TextButton(
            onClick = onBack,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (isPersian) "بازگشت به ورود حساب کاربری" else "Back to account sign-in",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Shown when the lock screen comes back for an open guest workspace; the form follows its security. */
@Composable
private fun GuestLockCard(
    isPersian: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    fingerprintEnabled: Boolean,
    passcodeEnabled: Boolean,
    showPasscode: Boolean,
    passcode: String,
    onPasscodeChange: (String) -> Unit,
    onFingerprintClick: () -> Unit,
    onUsePasscodeClick: () -> Unit,
    onPasscodeSubmit: () -> Unit,
    onContinueClick: () -> Unit,
    onAccountSignInClick: (() -> Unit)? = null
) {
    AuthCard {
        when {
            fingerprintEnabled && !showPasscode -> {
                LoadingButton(
                    text = if (isPersian) "ورود با اثر انگشت" else "Sign in with fingerprint",
                    isLoading = isLoading,
                    onClick = onFingerprintClick
                )
                if (passcodeEnabled) {
                    TextButton(
                        onClick = onUsePasscodeClick,
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isPersian) "ورود با گذرواژه مهمان" else "Use guest passcode instead",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            passcodeEnabled -> {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = onPasscodeChange,
                    enabled = !isLoading,
                    label = { Text(if (isPersian) "گذرواژه مهمان" else "Guest passcode") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = credentialTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, hintLocales = LocaleList(Locale("en"))),
                    keyboardActions = KeyboardActions(onDone = { onPasscodeSubmit() }),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                LoadingButton(
                    text = if (isPersian) "ورود" else "Unlock",
                    isLoading = isLoading,
                    onClick = onPasscodeSubmit
                )
            }
            else -> {
                // No secret was set up for the guest workspace, so there is nothing to verify.
                LoadingButton(
                    text = if (isPersian) "ادامه" else "Continue",
                    isLoading = isLoading,
                    onClick = onContinueClick
                )
            }
        }

        AnimatedVisibility(errorMessage != null) {
            errorMessage?.let { AuthErrorBanner(message = it) }
        }

        if (onAccountSignInClick != null) {
            TextButton(
                onClick = onAccountSignInClick,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isPersian) "ورود به حساب کاربری" else "Sign in to an account",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
private object LocalLoginBackPress { var current: Long = 0L }

@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loadingText: String? = null
) {
    val buttonShape = RoundedCornerShape(16.dp)

    // انیمیشن ترنزیشن بی‌نهایت برای جابه‌جایی افکت نوری لودینگ
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")

    // ۱. انیمیشن موقعیت افقی نور درخشش (از چپ به راست)
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    // ۲. انیمیشن پالس ملایم متن در حالت لودینگ
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    // تعریف گریدینت درخشش خطی بر اساس رنگ‌های تم نانو بنانا
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary, // #002114 یا #408A71
            Color(0xFFB0E4CC).copy(alpha = 0.8f), // رنگ درخشش روشن اسپلش
            MaterialTheme.colorScheme.primary
        ),
        start = Offset(shimmerTranslateX - 300f, 0f),
        end = Offset(shimmerTranslateX, 0f)
    )

    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ),
        enabled = !isLoading,
        contentPadding = PaddingValues(0.dp) // حذف پدینگ برای اعمال یکدست گریدینت
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLoading) Modifier.background(shimmerBrush)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLoading) (loadingText ?: if (LocaleHelper.getLanguage(LocalContext.current) == "fa") "در حال ورود..." else "Signing In...") else text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (isLoading) textAlpha else 1f)
            )
        }
    }
}