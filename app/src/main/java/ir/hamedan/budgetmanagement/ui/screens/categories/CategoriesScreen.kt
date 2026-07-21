package ir.hamedan.budgetmanagement.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamedan.budgetmanagement.data.local.models.CategoryEntity
import ir.hamedan.budgetmanagement.ui.components.AuroraBackground
import ir.hamedan.budgetmanagement.utils.LocaleHelper
import ir.hamedan.budgetmanagement.utils.StringMapper

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit = {},
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val isPersian = remember { LocaleHelper.getLanguage(context) == "fa" }

    val categoriesList by viewModel.categories.collectAsState()

    var selectedTabState by remember { mutableIntStateOf(0) }
    val isExpenseTab = selectedTabState == 0

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

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
            // هدر
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
                    text = if (isPersian) "مدیریت دسته‌بندی‌ها" else "Manage Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.size(44.dp))
            }

            // تب سوییچر (هزینه / درآمد)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { selectedTabState = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpenseTab) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                        contentColor = if (isExpenseTab) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "هزینه‌ها" else "Expenses", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedTabState = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isExpenseTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                        contentColor = if (!isExpenseTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPersian) "درآمدها" else "Incomes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredCategories = categoriesList.filter { it.isExpense == isExpenseTab }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    CategoryItemCard(
                        category = category,
                        isPersian = isPersian,
                        onEditClick = { categoryToEdit = category },
                        onDeleteClick = { categoryToDelete = category }
                    )
                }
            }
        }

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
            Icon(Icons.Default.Add, contentDescription = "Add Category", modifier = Modifier.size(28.dp))
        }

        if (showAddDialog || categoryToEdit != null) {
            AddOrEditCategoryDialog(
                categoryToEdit = categoryToEdit,
                isExpense = isExpenseTab,
                isPersian = isPersian,
                onDismiss = {
                    showAddDialog = false
                    categoryToEdit = null
                },
                onConfirm = { title, emoji ->
                    if (categoryToEdit != null) {
                        viewModel.updateCategory(categoryToEdit!!, title, emoji)
                    } else {
                        viewModel.addCategory(title, emoji, isExpenseTab)
                    }
                    showAddDialog = false
                    categoryToEdit = null
                }
            )
        }

        categoryToDelete?.let { category ->
            var transactionCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(category.title) {
                transactionCount = viewModel.getTransactionCount(category.title)
            }

            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = if (isPersian) "حذف دسته‌بندی" else "Delete Category",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val categoryName = StringMapper.getCategoryName(category.title, isPersian)
                    if (transactionCount > 0) {
                        Text(
                            text = if (isPersian)
                                "دسته‌بندی «$categoryName» دارای $transactionCount تراکنش ثبت‌شده است.\nبا حذف آن، این تراکنش‌ها به دسته‌بندی «دسته‌بندی نشده» منتقل می‌شوند. ادامه می‌دهید؟"
                            else
                                "'$categoryName' has $transactionCount active transactions.\nDeleting it will move these transactions to 'Uncategorized'. Continue?"
                        )
                    } else {
                        Text(
                            text = if (isPersian)
                                "آیا از حذف دسته‌بندی «$categoryName» اطمینان دارید؟"
                            else
                                "Are you sure you want to delete '$categoryName'?"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategoryWithReassignment(category)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "بله، حذف شود" else "Yes, Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { categoryToDelete = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryItemCard(
    category: CategoryEntity,
    isPersian: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                cardShape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                cardShape
            )
            .clip(cardShape)
            .clickable { onEditClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = category.iconEmoji, fontSize = 28.sp)

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = StringMapper.getCategoryName(category.title, isPersian),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AddOrEditCategoryDialog(
    categoryToEdit: CategoryEntity? = null,
    isExpense: Boolean,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, emoji: String) -> Unit
) {
    var title by remember { mutableStateOf(categoryToEdit?.title ?: "") }
    var emoji by remember { mutableStateOf(categoryToEdit?.iconEmoji ?: "🏷️") }

    val emojis = listOf(
        "🏷️", "🍕", "🚗", "🛍️", "📄", "💰", "📈", "🎮", "🏠", "✈️", "☕", "💻",
        "🛒", "🍔", "🥖", "🍏", "🍹", "🚌", "⛽", "🔧", "🚕", "💊", "🏥", "💈",
        "💅", "👕", "👟", "💡", "💧", "📶", "📱", "🎁", "🎓", "💎", "💳", "🏦"
    )

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(24.dp)

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, dialogShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), dialogShape)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (categoryToEdit != null) {
                        if (isPersian) "ویرایش دسته‌بندی" else "Edit Category"
                    } else {
                        if (isPersian) {
                            if (isExpense) "دسته‌بندی جدید هزینه" else "دسته‌بندی جدید درآمد"
                        } else {
                            if (isExpense) "New Expense Category" else "New Income Category"
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojis) { item ->
                        Text(
                            text = item,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { emoji = item }
                                .background(
                                    if (emoji == item) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isPersian) "عنوان دسته‌بندی" else "Category Title") },
                    leadingIcon = { Text(text = emoji, fontSize = 20.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isPersian) "انصراف" else "Cancel")
                    }

                    Button(
                        onClick = { onConfirm(title, emoji) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (categoryToEdit != null) {
                                if (isPersian) "ذخیره" else "Save"
                            } else {
                                if (isPersian) "افزودن" else "Add"
                            }
                        )
                    }
                }
            }
        }
    }
}