package ir.hamedan.budgetmanagement.ui.screens.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.R
import ir.hamedan.budgetmanagement.data.local.models.SavingGoalEntity
import ir.hamedan.budgetmanagement.data.preferences.CurrencySharedPreferences
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import java.text.NumberFormat
import java.util.Locale

// ایموجی‌های پیشنهادی برای انتخاب قلک
val DEFAULT_GOAL_EMOJIS = listOf("🎯", "🚗", "🏠", "💻", "📱", "✈️", "💍", "🎓", "🎮", "🎁", "🛵", "🪙")

/**
 * VisualTransformation جهت تفکیک سه رقمی اعداد بدون دستکاری متن اصلی
 * این روش مشکل پریدن کرسر را به طور کامل برطرف می‌کند.
 */
class CurrencyAmountInputVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedText = try {
            val parsed = originalText.toLong()
            NumberFormat.getNumberInstance(Locale.US).format(parsed)
        } catch (e: Exception) {
            originalText
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val digitsBefore = offset.coerceAtMost(originalText.length)
                var commasCount = 0
                val totalDigits = originalText.length

                for (i in 1..digitsBefore) {
                    val digitsFromRight = totalDigits - i
                    if (digitsFromRight > 0 && digitsFromRight % 3 == 0) {
                        commasCount++
                    }
                }
                return (digitsBefore + commasCount).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(formattedText.length)
                var commasBefore = 0
                for (i in 0 until safeOffset) {
                    if (formattedText[i] == ',') {
                        commasBefore++
                    }
                }
                return (safeOffset - commasBefore).coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

@Composable
fun SavingGoalsScreen(
    onBackClick: () -> Unit = {},
    viewModel: SavingGoalsViewModel = viewModel(factory = SavingGoalsViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val goalsListState by viewModel.savingGoals.collectAsState()
    val currencyUnit by CurrencySharedPreferences.currencyFlow.collectAsState()

    val numberFormatter = remember(isPersian) {
        NumberFormat.getNumberInstance(if (isPersian) Locale("fa", "IR") else Locale.US)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalForDeposit by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalForWithdraw by remember { mutableStateOf<SavingGoalEntity?>(null) }

    val goalsList = goalsListState ?: emptyList()
    val totalSaved = goalsList.sumOf { it.currentAmount }
    val totalTarget = goalsList.sumOf { it.targetAmount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AuroraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // هدر صفحه
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (isPersian) "مدیریت قلک‌ها" else "Savings Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(44.dp))
            }

            if (goalsListState != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // کارت خلاصه عملکرد کلی
                    item {
                        val summaryShape = RoundedCornerShape(24.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), summaryShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), summaryShape)
                                .clip(summaryShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.goalbanner),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.12f
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "مجموع ذخیره شده" else "Total Savings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))

                                val displayedSaved = if (currencyUnit == "IRR") totalSaved * 10 else totalSaved
                                val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")

                                Text(
                                    text = "${numberFormatter.format(displayedSaved.toLong())} $currencyText",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(12.dp))

                                val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
                                val animatedOverallProgress by animateFloatAsState(targetValue = overallProgress, label = "OverallProgress")

                                LinearProgressIndicator(
                                    progress = { animatedOverallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isPersian) "هدف کلی:" else "Total Target:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val displayedTarget = if (currencyUnit == "IRR") totalTarget * 10 else totalTarget
                                    Text(
                                        text = "${numberFormatter.format(displayedTarget.toLong())} $currencyText",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // حالت خالی
                    if (goalsList.isEmpty()) {
                        item {
                            val emptyCardShape = RoundedCornerShape(24.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), emptyCardShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), emptyCardShape)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "🪙", fontSize = 56.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (isPersian) "هنوز هیچ قلکی نساختی!" else "No Savings Goals Yet!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isPersian) "برای پس‌انداز هدفمند (خرید ماشین، سفر، لپ‌تاپ و...) همین الان اولین قلکت رو بساز."
                                        else "Start saving for your dreams (car, travel, tech) by creating your first goal.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPersian) "ساخت اولین قلک" else "Create First Goal",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = if (isPersian) "قلک‌های شما" else "Your Goals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(goalsList, key = { it.id }) { goal ->
                            SavingGoalItemCard(
                                goal = goal,
                                isPersian = isPersian,
                                currencyUnit = currencyUnit,
                                numberFormatter = numberFormatter,
                                onDepositClick = { goalForDeposit = goal },
                                onWithdrawClick = { goalForWithdraw = goal },
                                onEditClick = { goalToEdit = goal },
                                onDeleteClick = { goalToDelete = goal }
                            )
                        }
                    }
                }
            }
        }

        if (goalsListState != null && goalsList.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", modifier = Modifier.size(28.dp))
            }
        }

        // دیالوگ افزودن / ویرایش
        if (showAddDialog || goalToEdit != null) {
            AddOrEditGoalDialog(
                goalToEdit = goalToEdit,
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                onDismiss = {
                    showAddDialog = false
                    goalToEdit = null
                },
                onConfirm = { title, targetAmount, icon ->
                    if (goalToEdit != null) {
                        viewModel.updateGoal(goalToEdit!!.copy(title = title, targetAmount = targetAmount, icon = icon))
                    } else {
                        viewModel.addGoal(title, targetAmount, icon)
                    }
                    showAddDialog = false
                    goalToEdit = null
                }
            )
        }

        // دیالوگ واریز
        goalForDeposit?.let { goal ->
            AmountActionDialog(
                title = if (isPersian) "واریز به «${goal.title}»" else "Deposit to '${goal.title}'",
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isDeposit = true,
                onDismiss = { goalForDeposit = null },
                onConfirm = { amount ->
                    viewModel.deposit(goal.id, amount)
                    goalForDeposit = null
                }
            )
        }

        // دیالوگ برداشت
        goalForWithdraw?.let { goal ->
            AmountActionDialog(
                title = if (isPersian) "برداشت از «${goal.title}»" else "Withdraw from '${goal.title}'",
                isPersian = isPersian,
                currencyUnit = currencyUnit,
                isDeposit = false,
                onDismiss = { goalForWithdraw = null },
                onConfirm = { amount ->
                    viewModel.withdraw(goal.id, amount)
                    goalForWithdraw = null
                }
            )
        }

        // دیالوگ حذف با طراحی شیشه‌ای شیک
        goalToDelete?.let { goal ->
            val dialogShape = RoundedCornerShape(28.dp)
            Dialog(onDismissRequest = { goalToDelete = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            shape = dialogShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = dialogShape
                        )
                        .clip(dialogShape)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = if (isPersian) "حذف قلک" else "Delete Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isPersian)
                                "آیا از حذف قلک «${goal.title}» اطمینان دارید؟"
                            else
                                "Are you sure you want to delete '${goal.title}'?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { goalToDelete = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPersian) "انصراف" else "Cancel")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.deleteGoal(goal)
                                    goalToDelete = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPersian) "حذف" else "Delete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingGoalItemCard(
    goal: SavingGoalEntity,
    isPersian: Boolean,
    currencyUnit: String,
    numberFormatter: NumberFormat,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "GoalProgress")
    val percentText = (progress * 100).toInt()

    val currencyText = if (isPersian) (if (currencyUnit == "IRR") "ریال" else "تومان") else (if (currencyUnit == "IRR") "Rial" else "Toman")
    val currentDisp = if (currencyUnit == "IRR") goal.currentAmount * 10 else goal.currentAmount
    val targetDisp = if (currencyUnit == "IRR") goal.targetAmount * 10 else goal.targetAmount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), cardShape)
            .clip(cardShape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = goal.icon, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${numberFormatter.format(currentDisp.toLong())} / ${numberFormatter.format(targetDisp.toLong())} $currencyText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percentText%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPersian) "واریز" else "Deposit", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPersian) "برداشت" else "Withdraw", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun AddOrEditGoalDialog(
    goalToEdit: SavingGoalEntity? = null,
    isPersian: Boolean,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, icon: String) -> Unit
) {
    val maxDigits = 12
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var selectedIcon by remember { mutableStateOf(goalToEdit?.icon ?: "🎯") }

    val initialTargetRaw = remember(goalToEdit, currencyUnit) {
        if (goalToEdit == null) ""
        else {
            val amount = if (currencyUnit == "IRR") (goalToEdit.targetAmount * 10).toLong() else goalToEdit.targetAmount.toLong()
            amount.toString()
        }
    }
    // صرفاً ارقام خام در State نگهداری می‌شوند
    var rawTargetAmount by remember { mutableStateOf(initialTargetRaw) }

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                    shape = dialogShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = dialogShape
                )
                .clip(dialogShape)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (goalToEdit != null) {
                        if (isPersian) "ویرایش قلک" else "Edit Goal"
                    } else {
                        if (isPersian) "ساخت قلک جدید" else "New Savings Goal"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // انتخاب آیکون
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isPersian) "انتخاب آیکون:" else "Select Icon:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DEFAULT_GOAL_EMOJIS) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == emoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (selectedIcon == emoji) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedIcon = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 25) title = it },
                    label = { Text(if (isPersian) "عنوان قلک (مثلاً: خرید لپ‌تاپ)" else "Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                val labelCurrency = if (isPersian) (if (currencyUnit == "IRR") "مبلغ هدف (ریال)" else "مبلغ هدف (تومان)")
                else (if (currencyUnit == "IRR") "Target Amount (Rial)" else "Target Amount (Toman)")

                // ورود مبلغ با اعمال VisualTransformation و محدودیت ۱۲ رقم
                OutlinedTextField(
                    value = rawTargetAmount,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigits) {
                            rawTargetAmount = digitsOnly
                        }
                    },
                    label = { Text(labelCurrency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyAmountInputVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPersian) "انصراف" else "Cancel")
                        }
                    }

                    Button(
                        onClick = {
                            val cleanNumber = rawTargetAmount.toDoubleOrNull() ?: 0.0
                            val finalTargetInToman = if (currencyUnit == "IRR") cleanNumber / 10.0 else cleanNumber
                            onConfirm(title.trim(), finalTargetInToman, selectedIcon)
                        },
                        enabled = title.isNotBlank() && (rawTargetAmount.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (goalToEdit != null) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (goalToEdit != null) (if (isPersian) "ذخیره" else "Save")
                                else (if (isPersian) "ایجاد" else "Create"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmountActionDialog(
    title: String,
    isPersian: Boolean,
    currencyUnit: String,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    val maxDigits = 12
    var rawAmount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                    shape = dialogShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = dialogShape
                )
                .clip(dialogShape)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isDeposit) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                val labelCurrency = if (isPersian) (if (currencyUnit == "IRR") "مبلغ (ریال)" else "مبلغ (تومان)")
                else (if (currencyUnit == "IRR") "Amount (Rial)" else "Amount (Toman)")

                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= maxDigits) {
                            rawAmount = digitsOnly
                        }
                    },
                    label = { Text(labelCurrency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyAmountInputVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPersian) "انصراف" else "Cancel")
                        }
                    }

                    Button(
                        onClick = {
                            val cleanNumber = rawAmount.toDoubleOrNull() ?: 0.0
                            val finalAmountInToman = if (currencyUnit == "IRR") cleanNumber / 10.0 else cleanNumber
                            onConfirm(finalAmountInToman)
                        },
                        enabled = (rawAmount.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPersian) "تایید" else "Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}