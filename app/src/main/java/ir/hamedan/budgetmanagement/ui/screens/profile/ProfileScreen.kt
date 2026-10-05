package ir.hamedan.budgetmanagement.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.BudgetApp
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.ui.screens.settings.ChangePasswordDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class GenderOption(val value: String) {
    MALE("male"), FEMALE("female"), PREFER_NOT_TO_SAY("prefer_not_to_say")
}

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
    val isPersian = ir.hamedan.budgetmanagement.ui.theme.isPersianLocale()

    var firstName by remember(user?.firstName) { mutableStateOf(user?.firstName.orEmpty()) }
    var lastName by remember(user?.lastName) { mutableStateOf(user?.lastName.orEmpty()) }
    var gender by remember(user?.gender) { mutableStateOf(user?.gender ?: GenderOption.PREFER_NOT_TO_SAY.value) }
    var birthDate by remember(user?.birthDate) { mutableStateOf(user?.birthDate) }
    var email by remember(user?.email) { mutableStateOf(user?.email.orEmpty()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    val completion = remember(user, email) {
        listOf(
            firstName.isNotBlank(),
            lastName.isNotBlank(),
            gender != GenderOption.PREFER_NOT_TO_SAY.value,
            !birthDate.isNullOrBlank(),
            user?.emailVerified == true
        ).count { it }
    }

    LaunchedEffect(state.emailOtpSent) {
        if (state.emailOtpSent) {
            otpCode = ""
            showEmailDialog = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = if (isPersian) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                            contentDescription = if (isPersian) "بازگشت" else "Back"
                        )
                    }
                    Text(
                        if (isPersian) "پروفایل من" else "My Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = if (isPersian) "به‌روزرسانی" else "Refresh")
                    }
                }
            }

            item {
                ProfileHeroCard(
                    user = user,
                    gender = gender,
                    isPersian = isPersian,
                    completion = completion
                )
            }

            item {
                ProfileCompletionCard(
                    completion = completion,
                    isPersian = isPersian
                )
            }

            item {
                ProfileSectionTitle(
                    title = if (isPersian) "اطلاعات شخصی" else "Personal information",
                    subtitle = if (isPersian) "هر زمان خواستی می‌توانی این اطلاعات را ویرایش کنی." else "You can update these details whenever you want."
                )
            }

            item {
                ProfileTextField(
                    value = firstName,
                    onValueChange = { firstName = it.take(80) },
                    label = if (isPersian) "نام" else "First name",
                    icon = Icons.Default.Person
                )
            }

            item {
                ProfileTextField(
                    value = lastName,
                    onValueChange = { lastName = it.take(80) },
                    label = if (isPersian) "نام خانوادگی" else "Last name",
                    icon = Icons.Default.Badge
                )
            }

            item {
                ProfilePickerField(
                    value = genderLabel(gender, isPersian),
                    label = if (isPersian) "جنسیت" else "Gender",
                    icon = genderIcon(gender),
                    verified = gender != GenderOption.PREFER_NOT_TO_SAY.value,
                    onClick = { showGenderDialog = true }
                )
            }

            item {
                ProfilePickerField(
                    value = birthDate?.let { formatBirthDate(it, isPersian) }
                        ?: if (isPersian) "تاریخ تولد را اضافه کنید" else "Add your birth date",
                    label = if (isPersian) "تاریخ تولد" else "Date of birth",
                    icon = Icons.Default.Cake,
                    verified = !birthDate.isNullOrBlank(),
                    onClick = { showDatePicker = true }
                )
            }

            item {
                ProfileReadOnlyField(
                    value = maskPhone(user?.phoneNumber.orEmpty()),
                    label = if (isPersian) "شماره موبایل" else "Mobile number",
                    icon = Icons.Default.Phone,
                    supporting = if (isPersian) "برای امنیت حساب قابل تغییر نیست." else "This number cannot be changed for account security."
                )
            }

            item {
                ProfileEmailCard(
                    email = email,
                    verified = user?.emailVerified == true,
                    isPersian = isPersian,
                    onEdit = { showEmailDialog = true }
                )
            }

            item {
                Button(
                    onClick = { viewModel.saveProfile(firstName, lastName, gender, birthDate) },
                    enabled = !state.isSaving && firstName.isNotBlank() && lastName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPersian) "ذخیره تغییرات" else "Save changes")
                    }
                }
            }

            item {
                ProfileSectionTitle(
                    title = if (isPersian) "امنیت حساب" else "Account security",
                    subtitle = if (isPersian) "مدیریت گذرواژه و بازیابی حساب" else "Manage your password and account recovery"
                )
            }

            item {
                ProfileActionCard(
                    icon = Icons.Default.LockReset,
                    title = if (isPersian) "تغییر یا فراموشی رمز عبور" else "Change or forgot password",
                    subtitle = if (isPersian) "رمز فعلی را تغییر دهید یا وارد فرایند بازیابی شوید." else "Change your current password or start account recovery.",
                    onClick = { showChangePassword = true }
                )
            }

            if (state.error != null) {
                item {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text(if (isPersian) "جنسیت" else "Gender") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GenderOption.entries.forEach { option ->
                        val selected = gender == option.value
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    gender = option.value
                                    showGenderDialog = false
                                }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(genderIcon(option.value), null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(genderLabel(option.value, isPersian), modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.let(::isoToUtcMillis),
            yearRange = 1900..java.time.Year.now().value
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { birthDate = utcMillisToIso(it) }
                        showDatePicker = false
                    }
                ) { Text(if (isPersian) "تأیید" else "Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(if (isPersian) "انصراف" else "Cancel") } }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    if (showEmailDialog) {
        EmailDialog(
            isPersian = isPersian,
            initialEmail = email,
            otpSent = state.emailOtpSent,
            pendingEmail = state.emailPending,
            otpCode = otpCode,
            onOtpCodeChange = { otpCode = it.filter(Char::isDigit).take(6) },
            isSaving = state.isSaving,
            onDismiss = {
                showEmailDialog = false
                if (state.emailOtpSent) viewModel.dismissEmailOtp()
            },
            onRequestOtp = {
                val target = email.trim()
                if (target.isNotBlank()) viewModel.requestEmailVerification(target)
            },
            onVerify = {
                if (otpCode.length == 6) {
                    viewModel.verifyEmail(otpCode)
                    showEmailDialog = false
                }
            },
            onEmailChange = { email = it.take(320) }
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
}

@Composable
private fun ProfileHeroCard(user: UserEntity?, gender: String, isPersian: Boolean, completion: Int) {
    val icon = genderIcon(gender)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f))
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(76.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOf(user?.firstName, user?.lastName).filter { !it.isNullOrBlank() }.joinToString(" ")
                        .ifBlank { if (isPersian) "پروفایل شما" else "Your profile" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isPersian) "پروفایل شما $completion از ۵ بخش تکمیل شده" else "$completion of 5 profile sections completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileCompletionCard(completion: Int, isPersian: Boolean) {
    val progress = completion / 5f
    val message = when {
        completion == 0 -> if (isPersian) "با چند دقیقه تکمیلش کن؛ پروفایلت برای شخصی‌سازی آماده می‌شود." else "Take a few minutes to complete it and unlock a more personal experience."
        completion < 3 -> if (isPersian) "شروع خوبی است؛ فقط چند بخش مهم دیگر مانده." else "Good start. A few important details are still missing."
        completion < 5 -> if (isPersian) "تقریباً کامل شد؛ یک قدم دیگر تا پروفایل کامل." else "Almost there. One more step toward a complete profile."
        else -> if (isPersian) "پروفایل کامل است؛ از تجربه شخصی‌تر Cidna لذت ببر." else "Your profile is complete. Enjoy a more personal Cidna experience."
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isPersian) "پروفایل هوشمند" else "Smart profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("$completion/5", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        singleLine = true,
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun ProfilePickerField(value: String, label: String, icon: ImageVector, verified: Boolean, onClick: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = {
            Icon(
                if (verified) Icons.Default.CheckCircle else Icons.Default.ChevronLeft,
                null,
                tint = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun ProfileReadOnlyField(value: String, label: String, icon: ImageVector, supporting: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        supportingText = { Text(supporting) },
        trailingIcon = { Icon(Icons.Default.Lock, null) },
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun ProfileEmailCard(email: String, verified: Boolean, isPersian: Boolean, onEdit: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isPersian) "ایمیل" else "Email", style = MaterialTheme.typography.labelLarge)
                Text(
                    email.ifBlank { if (isPersian) "افزودن ایمیل" else "Add an email" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (verified) (if (isPersian) "تأیید شده" else "Verified")
                    else (if (isPersian) "نیازمند تأیید" else "Verification required"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Icon(Icons.Default.Edit, null)
        }
    }
}

@Composable
private fun ProfileActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronLeft, null)
        }
    }
}

@Composable
private fun EmailDialog(
    isPersian: Boolean,
    initialEmail: String,
    otpSent: Boolean,
    pendingEmail: String?,
    otpCode: String,
    onOtpCodeChange: (String) -> Unit,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRequestOtp: () -> Unit,
    onVerify: () -> Unit,
    onEmailChange: (String) -> Unit
) {
    var localEmail by remember(initialEmail) { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (otpSent) (if (isPersian) "تأیید ایمیل" else "Verify email") else (if (isPersian) "ایمیل حساب" else "Account email")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!otpSent) {
                    OutlinedTextField(
                        value = localEmail,
                        onValueChange = { localEmail = it; onEmailChange(it) },
                        label = { Text(if (isPersian) "آدرس ایمیل" else "Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text(
                        if (isPersian) "برای تغییر یا تأیید ایمیل، یک کد یک‌بارمصرف به همین آدرس ارسال می‌شود."
                        else "A one-time code will be sent to this address to verify or change it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        if (isPersian) "کد ۶ رقمی ارسال‌شده به ${pendingEmail.orEmpty()} را وارد کنید."
                        else "Enter the 6-digit code sent to ${pendingEmail.orEmpty()}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = onOtpCodeChange,
                        label = { Text(if (isPersian) "کد تأیید" else "Verification code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = if (otpSent) onVerify else onRequestOtp,
                enabled = !isSaving && (if (otpSent) otpCode.length == 6 else localEmail.contains("@"))
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (otpSent) (if (isPersian) "تأیید" else "Verify") else (if (isPersian) "ارسال کد" else "Send code"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") } }
    )
}

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

private fun formatBirthDate(value: String, isPersian: Boolean): String {
    return runCatching {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("d MMM yyyy", if (isPersian) Locale("fa") else Locale.US)
        output.format(input.parse(value) ?: return value)
    }.getOrDefault(value)
}

private fun isoToUtcMillis(value: String): Long? = runCatching {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(value)
    date?.time
}.getOrNull()

private fun utcMillisToIso(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(value))