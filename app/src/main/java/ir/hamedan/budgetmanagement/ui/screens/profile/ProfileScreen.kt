package ir.hamedan.budgetmanagement.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.auth.AuthInputNormalizer
import ir.hamedan.budgetmanagement.ui.screens.auth.credentialTextStyle
import ir.hamedan.budgetmanagement.ui.screens.settings.ChangePasswordDialog
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import ir.hamedan.budgetmanagement.utils.AppHapticType
import ir.hamedan.budgetmanagement.utils.DateUtils
import ir.hamedan.budgetmanagement.utils.rememberAppHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 *  Profile screen
 *
 *  Design idea: the profile is an "identity card" whose single memorable element
 *  is a segmented ring around the avatar. The ring has exactly five arcs, one per
 *  profile section (first name, last name, gender, birth date, verified e-mail).
 *  Arcs light up one after another when the screen opens and whenever a section
 *  gets completed, so the ring replaces the old, duplicated "x of 5" cards.
 *  Everything else stays quiet: grouped frosted sheets that reuse the card
 *  language of Settings and the auth screens, one floating save bar that only
 *  exists while there are unsaved edits.
 * ──────────────────────────────────────────────────────────────────────────── */

private enum class GenderOption(val value: String) {
    MALE("male"), FEMALE("female"), PREFER_NOT_TO_SAY("prefer_not_to_say")
}

/** The five sections that make up the completion ring, in ring order. */
private enum class ProfileStep { FIRST_NAME, LAST_NAME, GENDER, BIRTH_DATE, EMAIL }

private const val OTP_LENGTH = 6
private const val OTP_RESEND_SECONDS = 60L

private const val ITEM_HERO = "hero"
private const val ITEM_PERSONAL = "personal"
private const val ITEM_CONTACT = "contact"
private const val ITEM_SECURITY = "security"

/* ───────────────────────────── Form state ───────────────────────────────── */

private data class FormSnapshot(
    val first: String = "",
    val last: String = "",
    val gender: String = GenderOption.PREFER_NOT_TO_SAY.value,
    val birth: String? = null
)

/**
 * Holds the editable fields and remembers the last values they were synced with, so the
 * screen can tell real unsaved edits apart from a background refresh. A refresh never
 * overwrites what the person is typing.
 */
@Stable
private class ProfileFormState {
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var gender by mutableStateOf(GenderOption.PREFER_NOT_TO_SAY.value)
    var birthDate by mutableStateOf<String?>(null)

    private var base by mutableStateOf(FormSnapshot())
    private var seeded = false

    private val current: FormSnapshot
        get() = FormSnapshot(firstName.trim(), lastName.trim(), gender, birthDate.normalized())

    val isDirty: Boolean get() = current != base

    val isValid: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()

    fun sync(user: UserEntity) {
        val incoming = FormSnapshot(
            first = user.firstName.trim(),
            last = user.lastName.trim(),
            gender = user.gender.takeIf { g -> GenderOption.entries.any { it.value == g } }
                ?: GenderOption.PREFER_NOT_TO_SAY.value,
            birth = user.birthDate.normalized()
        )
        // Saving makes the form equal to the incoming profile, so that case re-bases too.
        if (!seeded || !isDirty || current == incoming) {
            load(incoming)
            seeded = true
        }
    }

    fun discard() = load(base)

    /** Called when a save succeeded: the form is the new baseline, whatever the server echoes back. */
    fun markSaved() {
        base = current
    }

    private fun load(snapshot: FormSnapshot) {
        firstName = snapshot.first
        lastName = snapshot.last
        gender = snapshot.gender
        birthDate = snapshot.birth
        base = snapshot
    }

    companion object {
        val Saver: Saver<ProfileFormState, Any> = listSaver(
            save = {
                listOf(
                    it.firstName, it.lastName, it.gender, it.birthDate.orEmpty(),
                    it.base.first, it.base.last, it.base.gender, it.base.birth.orEmpty(),
                    it.seeded
                )
            },
            restore = { saved ->
                ProfileFormState().apply {
                    firstName = saved[0] as String
                    lastName = saved[1] as String
                    gender = saved[2] as String
                    birthDate = (saved[3] as String).normalized()
                    base = FormSnapshot(
                        saved[4] as String,
                        saved[5] as String,
                        saved[6] as String,
                        (saved[7] as String).normalized()
                    )
                    seeded = saved[8] as Boolean
                }
            }
        )
    }
}

private fun String?.normalized(): String? = this?.takeIf { it.isNotBlank() }

/* ─────────────────────────────── Screen ─────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BudgetApp
    val viewModel: ProfileViewModel = viewModel(factory = app.container.viewModelFactory())
    val state by viewModel.state.collectAsState()
    val user = state.user
    val isPersian = isPersianLocale()
    val scope = rememberCoroutineScope()

    val form = rememberSaveable(saver = ProfileFormState.Saver) {
        ProfileFormState().also { f -> user?.let(f::sync) }
    }
    LaunchedEffect(user) { user?.let(form::sync) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showGenderSheet by rememberSaveable { mutableStateOf(false) }
    var showEmailDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePassword by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val snackbarHost = remember { SnackbarHostState() }
    val firstNameFocus = remember { FocusRequester() }
    val lastNameFocus = remember { FocusRequester() }

    // Completion is measured on what is saved, so the ring moves when a save succeeds.
    val steps = remember(user) {
        listOf(
            !user?.firstName.isNullOrBlank(),
            !user?.lastName.isNullOrBlank(),
            user?.gender?.let { it == GenderOption.MALE.value || it == GenderOption.FEMALE.value } == true,
            !user?.birthDate.isNullOrBlank(),
            user?.emailVerified == true
        )
    }
    val completion = steps.count { it }

    val requestBack: () -> Unit = {
        if (form.isDirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = form.isDirty) { showDiscardDialog = true }

    LaunchedEffect(state.emailOtpSent) {
        if (state.emailOtpSent) showEmailDialog = true
    }

    // One-shot confirmations (saved / e-mail verified).
    val notice = state.notice
    LaunchedEffect(notice) {
        if (notice == null) return@LaunchedEffect
        if (notice == ProfileNotice.EMAIL_VERIFIED) showEmailDialog = false
        if (notice == ProfileNotice.PROFILE_SAVED) form.markSaved()
        snackbarHost.showSnackbar(
            when (notice) {
                ProfileNotice.PROFILE_SAVED -> if (isPersian) "تغییرات ذخیره شد" else "Changes saved"
                ProfileNotice.EMAIL_VERIFIED -> if (isPersian) "ایمیل شما تأیید شد" else "Email verified"
            }
        )
        viewModel.consumeNotice()
    }

    // Errors go to the snackbar, except while the e-mail dialog is open: the dialog shows
    // its own error next to the field that failed (a snackbar would sit behind its scrim).
    val error = state.error
    LaunchedEffect(error, showEmailDialog) {
        if (error != null && !showEmailDialog) {
            snackbarHost.showSnackbar(error)
            viewModel.clearError()
        }
    }

    fun focusName(target: FocusRequester) {
        scope.launch {
            listState.animateScrollToItem(1)
            runCatching { target.requestFocus() }
        }
    }

    // This screen is not inside a Surface/Scaffold, so Compose's default content color
    // (black) would apply to every Text/Icon without an explicit color. In dark mode that
    // made the titles unreadable, so the content color is set to the theme's onSurface.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxSize().imePadding()) {
            AuroraBackground()

            Column(Modifier.fillMaxSize()) {
                ProfileTopBar(
                    isPersian = isPersian,
                    isRefreshing = state.isLoading,
                    onBack = requestBack,
                    onRefresh = viewModel::refresh
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item(key = ITEM_HERO) {
                        ProfileHero(
                            firstName = form.firstName,
                            lastName = form.lastName,
                            steps = steps,
                            completion = completion,
                            isPersian = isPersian,
                            onStepClick = { step ->
                                when (step) {
                                    ProfileStep.FIRST_NAME -> focusName(firstNameFocus)
                                    ProfileStep.LAST_NAME -> focusName(lastNameFocus)
                                    ProfileStep.GENDER -> showGenderSheet = true
                                    ProfileStep.BIRTH_DATE -> showDatePicker = true
                                    ProfileStep.EMAIL -> showEmailDialog = true
                                }
                            }
                        )
                    }

                    item(key = ITEM_PERSONAL) {
                        PersonalInfoSheet(
                            form = form,
                            isPersian = isPersian,
                            firstNameFocus = firstNameFocus,
                            lastNameFocus = lastNameFocus,
                            onGenderClick = { showGenderSheet = true },
                            onBirthClick = { showDatePicker = true }
                        )
                    }

                    item(key = ITEM_CONTACT) {
                        ContactSheet(
                            phone = user?.phoneNumber.orEmpty(),
                            email = user?.email.orEmpty(),
                            emailVerified = user?.emailVerified == true,
                            isPersian = isPersian,
                            onEmailClick = { showEmailDialog = true }
                        )
                    }

                    item(key = ITEM_SECURITY) {
                        ProfileSheet(
                            title = if (isPersian) "امنیت حساب" else "Account security",
                            subtitle = if (isPersian) "مدیریت گذرواژه و بازیابی حساب" else "Manage your password and account recovery"
                        ) {
                            ProfileRow(
                                icon = Icons.Default.LockReset,
                                label = if (isPersian) "تغییر یا فراموشی رمز عبور" else "Change or forgot password",
                                value = if (isPersian) "رمز فعلی را تغییر دهید یا وارد فرایند بازیابی شوید." else "Change your current password or start account recovery.",
                                valueIsHint = true,
                                onClick = { showChangePassword = true }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SnackbarHost(hostState = snackbarHost, modifier = Modifier.padding(horizontal = 16.dp))
                AnimatedVisibility(
                    visible = form.isDirty,
                    enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
                ) {
                    SaveBar(
                        isPersian = isPersian,
                        isSaving = state.isSaving,
                        canSave = form.isValid && !state.isSaving,
                        onDiscard = form::discard,
                        onSave = { viewModel.saveProfile(form.firstName, form.lastName, form.gender, form.birthDate) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showGenderSheet) {
        GenderSheet(
            isPersian = isPersian,
            selected = form.gender,
            onSelect = { form.gender = it },
            onDismiss = { showGenderSheet = false }
        )
    }

    if (showDatePicker) {
        BirthDateSheet(
            isPersian = isPersian,
            initialIso = form.birthDate,
            onConfirm = { form.birthDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showEmailDialog) {
        EmailDialog(
            isPersian = isPersian,
            currentEmail = user?.email.orEmpty(),
            currentVerified = user?.emailVerified == true,
            otpSent = state.emailOtpSent,
            challengeId = state.emailOtpChallengeId,
            pendingEmail = state.emailPending,
            error = state.error,
            isSaving = state.isSaving,
            onClearError = viewModel::clearError,
            onDismiss = {
                showEmailDialog = false
                viewModel.clearError()
                if (state.emailOtpSent) viewModel.dismissEmailOtp()
            },
            onRequestOtp = viewModel::requestEmailVerification,
            onVerify = viewModel::verifyEmail,
            onChangeEmail = viewModel::dismissEmailOtp
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            isPersian = isPersian,
            onDismiss = { showChangePassword = false },
            onSubmit = { current, new ->
                app.container.authRepository.changePassword(current, new)
            },
            onSuccess = {
                showChangePassword = false
            },
            onForgotPassword = {
                showChangePassword = false
                onForgotPassword()
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    if (isPersian) "تغییرات ذخیره نشده" else "Unsaved changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isPersian) "اگر خارج شوید، تغییرات شما ذخیره نمی‌شود."
                    else "If you leave now, your changes won't be saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(if (isPersian) "ماندن" else "Keep editing")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        form.discard()
                        onBack()
                    }
                ) {
                    Text(
                        if (isPersian) "خروج بدون ذخیره" else "Leave without saving",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

/* ───────────────────────────── Top bar ──────────────────────────────────── */

@Composable
private fun ProfileTopBar(
    isPersian: Boolean,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isPersian) "بازگشت" else "Back"
            )
        }
        Text(
            text = if (isPersian) "پروفایل من" else "My Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (isRefreshing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = if (isPersian) "به‌روزرسانی" else "Refresh")
                }
            }
        }
    }
}

/* ────────────────────────────── Hero ────────────────────────────────────── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHero(
    firstName: String,
    lastName: String,
    steps: List<Boolean>,
    completion: Int,
    isPersian: Boolean,
    onStepClick: (ProfileStep) -> Unit
) {
    val total = ProfileStep.entries.size
    val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").trim()
    val countText = if (isPersian) "${persianDigits(completion.toString())} از ${persianDigits(total.toString())}" else "$completion of $total"
    val statusText = if (isPersian) "پروفایل شما $countText بخش تکمیل شده" else "$countText profile sections completed"
    val message = when {
        completion == 0 -> if (isPersian) "با چند دقیقه تکمیلش کن؛ پروفایلت برای شخصی‌سازی آماده می‌شود." else "Take a few minutes to complete it and unlock a more personal experience."
        completion < 3 -> if (isPersian) "شروع خوبی است؛ فقط چند بخش مهم دیگر مانده." else "Good start. A few important details are still missing."
        completion < total -> if (isPersian) "تقریباً کامل شد؛ یک قدم دیگر تا پروفایل کامل." else "Almost there. One more step toward a complete profile."
        else -> if (isPersian) "پروفایل کامل است؛ از تجربه شخصی‌تر Cidna لذت ببر." else "Your profile is complete. Enjoy a more personal Cidna experience."
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SegmentedAvatar(
            initials = initialsOf(firstName, lastName, isPersian),
            steps = steps,
            complete = completion == total,
            description = statusText
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = fullName.ifBlank { if (isPersian) "پروفایل شما" else "Your profile" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        val missing = ProfileStep.entries.filterIndexed { index, _ -> !steps[index] }
        if (missing.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                missing.forEach { step ->
                    MissingStepChip(
                        label = stepLabel(step, isPersian),
                        icon = stepIcon(step),
                        onClick = { onStepClick(step) }
                    )
                }
            }
        }
    }
}

/** Avatar with a five-arc completion ring; arcs fill in sequence the first time they appear. */
@Composable
private fun SegmentedAvatar(
    initials: String,
    steps: List<Boolean>,
    complete: Boolean,
    description: String
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fills = remember { List(ProfileStep.entries.size) { Animatable(0f) } }
    steps.forEachIndexed { index, done ->
        LaunchedEffect(done) {
            fills[index].animateTo(
                targetValue = if (done) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 520,
                    delayMillis = if (done) 140 + index * 110 else 0,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Box(
        modifier = Modifier.size(136.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val gap = 12f
            val slice = 360f / fills.size
            fills.forEachIndexed { index, fill ->
                val start = -90f + index * slice + gap / 2
                val sweep = slice - gap
                drawArc(
                    color = track,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (fill.value > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = start,
                        sweepAngle = sweep * fill.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        AnimatedVisibility(
            visible = complete,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp),
            enter = scaleIn(tween(300, delayMillis = 700)) + fadeIn(tween(300, delayMillis = 700)),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(3.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun MissingStepChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/* ───────────────────────────── Sheets (groups) ──────────────────────────── */

@Composable
private fun ProfileSheet(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 6.dp).padding(bottom = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), shape)
                .clip(shape),
            content = content
        )
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    )
}

@Composable
private fun PersonalInfoSheet(
    form: ProfileFormState,
    isPersian: Boolean,
    firstNameFocus: FocusRequester,
    lastNameFocus: FocusRequester,
    onGenderClick: () -> Unit,
    onBirthClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    ProfileSheet(
        title = if (isPersian) "اطلاعات شخصی" else "Personal information",
        subtitle = if (isPersian) "هر زمان خواستی می‌توانی این اطلاعات را ویرایش کنی." else "You can update these details whenever you want."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NameField(
                value = form.firstName,
                onValueChange = { form.firstName = it.take(80) },
                label = if (isPersian) "نام" else "First name",
                requiredText = if (isPersian) "الزامی" else "Required",
                focusRequester = firstNameFocus,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.weight(1f)
            )
            NameField(
                value = form.lastName,
                onValueChange = { form.lastName = it.take(80) },
                label = if (isPersian) "نام خانوادگی" else "Last name",
                requiredText = if (isPersian) "الزامی" else "Required",
                focusRequester = lastNameFocus,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() },
                modifier = Modifier.weight(1f)
            )
        }
        SheetDivider()
        val genderChosen = form.gender != GenderOption.PREFER_NOT_TO_SAY.value
        ProfileRow(
            icon = genderIcon(form.gender),
            label = if (isPersian) "جنسیت" else "Gender",
            value = genderLabel(form.gender, isPersian),
            done = genderChosen,
            actionText = if (isPersian) "انتخاب" else "Choose",
            onClick = onGenderClick
        )
        SheetDivider()
        val hasBirth = !form.birthDate.isNullOrBlank()
        ProfileRow(
            icon = Icons.Default.Cake,
            label = if (isPersian) "تاریخ تولد" else "Date of birth",
            value = form.birthDate?.let { formatBirthDate(it, isPersian) }
                ?: if (isPersian) "تاریخ تولد را اضافه کنید" else "Add your birth date",
            valueIsHint = !hasBirth,
            done = hasBirth,
            actionText = if (isPersian) "افزودن" else "Add",
            onClick = onBirthClick
        )
    }
}

@Composable
private fun ContactSheet(
    phone: String,
    email: String,
    emailVerified: Boolean,
    isPersian: Boolean,
    onEmailClick: () -> Unit
) {
    ProfileSheet(
        title = if (isPersian) "راه‌های ارتباطی" else "Contact",
        subtitle = null
    ) {
        ProfileRow(
            icon = Icons.Default.Phone,
            label = if (isPersian) "شماره موبایل" else "Mobile number",
            value = maskPhone(phone),
            valueIsLtr = true,
            supporting = if (isPersian) "برای امنیت حساب قابل تغییر نیست." else "This number cannot be changed for account security.",
            trailing = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        )
        SheetDivider()
        ProfileRow(
            icon = Icons.Default.Email,
            label = if (isPersian) "ایمیل" else "Email",
            value = email.ifBlank { if (isPersian) "افزودن ایمیل" else "Add an email" },
            valueIsHint = email.isBlank(),
            valueIsLtr = email.isNotBlank(),
            trailing = {
                when {
                    email.isBlank() -> ActionPill(if (isPersian) "افزودن" else "Add")
                    emailVerified -> StatusPill(
                        text = if (isPersian) "تأیید شده" else "Verified",
                        icon = Icons.Default.CheckCircle,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        content = MaterialTheme.colorScheme.primary
                    )
                    else -> StatusPill(
                        text = if (isPersian) "نیازمند تأیید" else "Verification required",
                        icon = null,
                        container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        content = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.width(4.dp))
                RowChevron()
            },
            onClick = onEmailClick
        )
    }
}

/**
 * One line inside a sheet: icon badge, small label, value, and an optional trailing slot.
 * When [trailing] is not supplied the row shows a check (when [done]) or an [actionText]
 * pill, followed by a chevron if it is tappable.
 */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueIsHint: Boolean = false,
    valueIsLtr: Boolean = false,
    done: Boolean = false,
    actionText: String? = null,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 68.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val valueStyle = if (valueIsHint) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
            Text(
                text = value,
                style = if (valueIsLtr) valueStyle.copy(textDirection = TextDirection.Ltr) else valueStyle,
                fontWeight = if (valueIsHint) FontWeight.Normal else FontWeight.SemiBold,
                color = if (valueIsHint) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.onSurface,
                maxLines = if (valueIsHint) 3 else 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else {
            if (done) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (actionText != null) {
                ActionPill(actionText)
            }
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                RowChevron()
            }
        }
    }
}

@Composable
private fun RowChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    )
}

@Composable
private fun ActionPill(text: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusPill(text: String, icon: ImageVector?, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = content)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = content)
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    requiredText: String,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A field only complains after the person has been in it and left it empty.
    var wasFocused by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    val showError = touched && value.isBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
                if (wasFocused && !focus.isFocused) touched = true
                wasFocused = focus.isFocused
            },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = showError,
        supportingText = if (showError) ({ Text(requiredText) }) else null,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() })
    )
}

/* ──────────────────────────── Save bar ──────────────────────────────────── */

@Composable
private fun SaveBar(
    isPersian: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .shadow(10.dp, shape, clip = false),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onDiscard, enabled = !isSaving) {
                Text(if (isPersian) "بازگردانی" else "Discard")
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPersian) "ذخیره تغییرات" else "Save changes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ─────────────────────────── Gender sheet ───────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderSheet(
    isPersian: Boolean,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (isPersian) "جنسیت" else "Gender",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            GenderOption.entries.forEach { option ->
                val isSelected = selected == option.value
                val shape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            shape
                        )
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            onSelect(option.value)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                        .heightIn(min = 60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(genderIcon(option.value), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        genderLabel(option.value, isPersian),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/* ─────────────────────────── E-mail dialog ──────────────────────────────── */

@Composable
private fun EmailDialog(
    isPersian: Boolean,
    currentEmail: String,
    currentVerified: Boolean,
    otpSent: Boolean,
    challengeId: String?,
    pendingEmail: String?,
    error: String?,
    isSaving: Boolean,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onRequestOtp: (String) -> Unit,
    onVerify: (String) -> Unit,
    onChangeEmail: () -> Unit
) {
    var localEmail by remember { mutableStateOf(currentEmail) }
    var code by remember(challengeId) { mutableStateOf("") }

    // Resend cooldown, restarted every time a new code is issued.
    val sentAt = remember(challengeId) { System.currentTimeMillis() }
    var now by remember(challengeId) { mutableLongStateOf(sentAt) }
    LaunchedEffect(challengeId, otpSent) {
        if (!otpSent) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val cooldown = (OTP_RESEND_SECONDS - (now - sentAt) / 1000).coerceAtLeast(0)

    val trimmed = localEmail.trim()
    val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    val alreadyVerified = currentVerified && trimmed.equals(currentEmail, ignoreCase = true)
    val canSubmit = !isSaving && (if (otpSent) code.length == OTP_LENGTH else (validEmail && !alreadyVerified))

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = if (otpSent) Icons.Default.MarkEmailRead else Icons.Default.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                if (otpSent) (if (isPersian) "تأیید ایمیل" else "Verify email")
                else (if (isPersian) "ایمیل حساب" else "Account email"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!otpSent) {
                    Text(
                        if (isPersian) "برای تغییر یا تأیید ایمیل، یک کد یک‌بارمصرف به همین آدرس ارسال می‌شود."
                        else "A one-time code will be sent to this address to verify or change it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = localEmail,
                        onValueChange = { localEmail = it.take(320); onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isPersian) "آدرس ایمیل" else "Email address") },
                        singleLine = true,
                        textStyle = credentialTextStyle(),
                        shape = RoundedCornerShape(16.dp),
                        isError = alreadyVerified.not() && trimmed.isNotEmpty() && !validEmail,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onRequestOtp(trimmed) })
                    )
                    if (alreadyVerified) {
                        Text(
                            if (isPersian) "این ایمیل قبلاً تأیید شده است." else "This email is already verified.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        if (isPersian) "کد ۶ رقمی ارسال‌شده به ${pendingEmail.orEmpty()} را وارد کنید."
                        else "Enter the 6-digit code sent to ${pendingEmail.orEmpty()}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OtpField(
                        code = code,
                        onCodeChange = { code = AuthInputNormalizer.otpCode(it, OTP_LENGTH); onClearError() },
                        onDone = { if (canSubmit) onVerify(code) },
                        isError = error != null,
                        enabled = !isSaving
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { pendingEmail?.let(onRequestOtp) },
                            enabled = cooldown == 0L && !isSaving && pendingEmail != null
                        ) {
                            Text(
                                if (cooldown > 0) (if (isPersian) "ارسال مجدد تا ${persianDigits(cooldown.toString())} ثانیه دیگر" else "Resend in ${cooldown}s")
                                else (if (isPersian) "ارسال مجدد کد" else "Resend code")
                            )
                        }
                        TextButton(onClick = onChangeEmail, enabled = !isSaving) {
                            Text(if (isPersian) "تغییر ایمیل" else "Change email")
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (otpSent) onVerify(code) else onRequestOtp(trimmed) },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        if (otpSent) (if (isPersian) "تأیید" else "Verify")
                        else (if (isPersian) "ارسال کد" else "Send code")
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}

/** Six separate digit cells driven by one invisible text field (always laid out left-to-right). */
@Composable
private fun OtpField(
    code: String,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
    isError: Boolean,
    enabled: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    // The real field stays invisible; it is only here to receive focus and input.
                    Box(Modifier.size(1.dp)) { innerTextField() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        repeat(OTP_LENGTH) { index ->
                            val active = focused && index == code.length.coerceAtMost(OTP_LENGTH - 1)
                            val borderColor = when {
                                isError -> MaterialTheme.colorScheme.error
                                active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            }
                            val shape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape)
                                    .border(if (active || isError) 2.dp else 1.dp, borderColor, shape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code.getOrNull(index)?.toString().orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

/* ───────────────────────────── Helpers ──────────────────────────────────── */

private fun stepLabel(step: ProfileStep, isPersian: Boolean): String = when (step) {
    ProfileStep.FIRST_NAME -> if (isPersian) "نام" else "First name"
    ProfileStep.LAST_NAME -> if (isPersian) "نام خانوادگی" else "Last name"
    ProfileStep.GENDER -> if (isPersian) "جنسیت" else "Gender"
    ProfileStep.BIRTH_DATE -> if (isPersian) "تاریخ تولد" else "Date of birth"
    ProfileStep.EMAIL -> if (isPersian) "تأیید ایمیل" else "Verify email"
}

private fun stepIcon(step: ProfileStep): ImageVector = when (step) {
    ProfileStep.FIRST_NAME -> Icons.Default.Person
    ProfileStep.LAST_NAME -> Icons.Default.Badge
    ProfileStep.GENDER -> Icons.Default.PersonOutline
    ProfileStep.BIRTH_DATE -> Icons.Default.Cake
    ProfileStep.EMAIL -> Icons.Default.Email
}

private fun initialsOf(first: String, last: String, isPersian: Boolean): String {
    val a = first.trim().firstOrNull { it.isLetter() }
    val b = last.trim().firstOrNull { it.isLetter() }
    // A zero-width non-joiner keeps two Persian letters from joining into one shape.
    return listOfNotNull(a, b).joinToString(if (isPersian) "\u200C" else "") { it.uppercase() }
}

private fun persianDigits(value: String): String = TransactionShareFormatter.toPersianDigits(value)

private fun genderLabel(value: String, isPersian: Boolean): String = when (value) {
    "male" -> if (isPersian) "پسر / مرد" else "Male"
    "female" -> if (isPersian) "دختر / زن" else "Female"
    else -> if (isPersian) "ترجیح می‌دهم نگویم" else "Prefer not to say"
}

private fun genderIcon(value: String): ImageVector = when (value) {
    "male" -> Icons.Default.Male
    "female" -> Icons.Default.Female
    else -> Icons.Default.PersonOutline
}

private fun maskPhone(phone: String): String {
    if (phone.length < 7) return phone
    return phone.take(3) + "••••••" + phone.takeLast(2)
}

/** Persian UI shows the Jalali date, English UI the Gregorian one. [value] is stored as ISO yyyy-MM-dd. */
private fun formatBirthDate(value: String, isPersian: Boolean): String = runCatching {
    val date = LocalDate.parse(value)
    if (isPersian) {
        val (y, m, d) = DateUtils.toJalali(date)
        "${persianDigits(d.toString())} ${DateUtils.PERSIAN_MONTH_NAMES[m - 1]} ${persianDigits(y.toString())}"
    } else {
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    }
}.getOrDefault(value)

/* ───────────────────── Birth date: calendar maths ─────────────────────── */

private data class YMD(val y: Int, val m: Int, val d: Int)

private fun fieldsOf(date: LocalDate, jalali: Boolean): YMD =
    if (jalali) DateUtils.toJalali(date).let { YMD(it.first, it.second, it.third) }
    else YMD(date.year, date.monthValue, date.dayOfMonth)

private fun daysIn(y: Int, m: Int, jalali: Boolean): Int =
    if (jalali) DateUtils.getDaysInJalaliMonth(y, m) else java.time.YearMonth.of(y, m).lengthOfMonth()

/**
 * Jalali -> Gregorian by inverting the app's own [DateUtils.toJalali], so a date picked here
 * always displays back as exactly the same Jalali date everywhere else in the app.
 */
private fun dateOf(f: YMD, jalali: Boolean): LocalDate? {
    if (!jalali) return runCatching { LocalDate.of(f.y, f.m, f.d) }.getOrNull()
    var day = f.d
    while (day >= 1) {
        val target = f.y * 10000 + f.m * 100 + day
        val monthOffset = if (f.m <= 7) (f.m - 1) * 31L else 186L + (f.m - 7) * 30L
        var candidate = LocalDate.of(f.y + 621, 3, 21).plusDays(monthOffset + day - 1)
        repeat(8) {
            val (jy, jm, jd) = DateUtils.toJalali(candidate)
            val key = jy * 10000 + jm * 100 + jd
            if (key == target) return candidate
            candidate = candidate.plusDays(if (key < target) 1 else -1)
        }
        day-- // that day does not exist in this month/year (e.g. 30 Esfand); try the previous one
    }
    return null
}

/* ───────────────────── Birth date: wheel picker sheet ─────────────────── */

private class WheelSpec(
    val label: String,
    val items: List<String>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit,
    val weight: Float
)

/**
 * Three snapping wheels (day / month / year). The calendar follows the app language: Jalali with
 * Persian month names and digits in Persian, Gregorian in English. Future dates are not selectable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateSheet(
    isPersian: Boolean,
    initialIso: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val jalali = isPersian

    val today = remember { LocalDate.now() }
    val max = remember(jalali) { fieldsOf(today, jalali) }
    val minYear = if (jalali) 1279 else 1900

    fun clamp(v: YMD): YMD {
        val y = v.y.coerceIn(minYear, max.y)
        val m = v.m.coerceIn(1, if (y == max.y) max.m else 12)
        var dayMax = daysIn(y, m, jalali)
        if (y == max.y && m == max.m) dayMax = minOf(dayMax, max.d)
        return YMD(y, m, v.d.coerceIn(1, dayMax))
    }

    var picked by remember {
        val start = initialIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today.minusYears(25)
        mutableStateOf(clamp(fieldsOf(start, jalali)))
    }

    fun num(n: Int) = if (isPersian) persianDigits(n.toString()) else n.toString()

    val monthCount = if (picked.y == max.y) max.m else 12
    var dayCount = daysIn(picked.y, picked.m, jalali)
    if (picked.y == max.y && picked.m == max.m) dayCount = minOf(dayCount, max.d)
    val monthNames = if (jalali) DateUtils.PERSIAN_MONTH_NAMES else DateUtils.ENGLISH_MONTH_NAMES

    val dayWheel = WheelSpec(
        label = if (isPersian) "روز" else "Day",
        items = (1..dayCount).map(::num),
        selectedIndex = picked.d - 1,
        onSelect = { picked = clamp(picked.copy(d = it + 1)) },
        weight = 1f
    )
    val monthWheel = WheelSpec(
        label = if (isPersian) "ماه" else "Month",
        items = (1..monthCount).map { monthNames[it - 1] },
        selectedIndex = picked.m - 1,
        onSelect = { picked = clamp(picked.copy(m = it + 1)) },
        weight = 1.5f
    )
    val yearWheel = WheelSpec(
        label = if (isPersian) "سال" else "Year",
        items = (minYear..max.y).map(::num),
        selectedIndex = picked.y - minYear,
        onSelect = { picked = clamp(picked.copy(y = minYear + it)) },
        weight = 1.2f
    )
    // Day first in Persian (۲۵ مهر ۱۳۷۵), month first in English (Oct 25, 1996).
    val wheels = if (isPersian) listOf(dayWheel, monthWheel, yearWheel) else listOf(monthWheel, dayWheel, yearWheel)

    val pickedDate = remember(picked, jalali) { dateOf(picked, jalali) }
    val itemHeight = 44.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isPersian) "تاریخ تولد" else "Date of birth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isPersian) "تقویم شمسی" else "Gregorian calendar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = pickedDate?.let { formatBirthDate(it.toString(), isPersian) }.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            CompositionLocalProvider(
                LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    wheels.forEach { wheel ->
                        Text(
                            wheel.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(wheel.weight)
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(itemHeight * 5)) {
                    val band = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(itemHeight)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), band)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), band)
                    )
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        wheels.forEach { wheel ->
                            WheelPicker(
                                items = wheel.items,
                                selectedIndex = wheel.selectedIndex,
                                onSelected = wheel.onSelect,
                                itemHeight = itemHeight,
                                modifier = Modifier.weight(wheel.weight)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    pickedDate?.let { onConfirm(it.toString()) }
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                enabled = pickedDate != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isPersian) "تأیید" else "Confirm", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            ) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    }
}

/**
 * One snapping wheel. The centred row is the selection; neighbours fade with distance.
 * Selection is reported once the wheel has settled, and the wheel follows [selectedIndex]
 * when the parent changes it (for example when a shorter month clamps the day).
 */
@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    itemHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val lastIndex = (items.size - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val currentOnSelected by rememberUpdatedState(onSelected)

    // The list has two rows of padding above and below, so scroll position 0 centres item 0.
    val centerIndex by remember(items.size) {
        derivedStateOf {
            (listState.firstVisibleItemIndex + (listState.firstVisibleItemScrollOffset / itemPx).roundToInt())
                .coerceIn(0, lastIndex)
        }
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow { if (listState.isScrollInProgress) -1 else centerIndex }
            .filter { it >= 0 }
            .distinctUntilChanged()
            .collect { currentOnSelected(it) }
    }
    LaunchedEffect(listState, items.size) {
        snapshotFlow { centerIndex }.drop(1).collect { haptics.perform(AppHapticType.Tick) }
    }
    LaunchedEffect(selectedIndex, items.size) {
        if (!listState.isScrollInProgress && selectedIndex in items.indices && selectedIndex != centerIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = itemHeight * 2)
    ) {
        itemsIndexed(items) { index, label ->
            val distance = abs(index - centerIndex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { scope.launch { listState.animateScrollToItem(index) } },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = when (distance) {
                            0 -> 1f
                            1 -> 0.55f
                            2 -> 0.28f
                            else -> 0.12f
                        }
                    ),
                    maxLines = 1
                )
            }
        }
    }
}