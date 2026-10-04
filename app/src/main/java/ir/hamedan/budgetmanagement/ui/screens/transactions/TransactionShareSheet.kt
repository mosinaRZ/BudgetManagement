package ir.hamedan.budgetmanagement.ui.screens.transactions

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ir.hamedan.budgetmanagement.data.local.models.TransactionEntity
import ir.hamedan.budgetmanagement.data.share.TransactionShareCardRenderer
import ir.hamedan.budgetmanagement.data.share.TransactionShareFormatter
import ir.hamedan.budgetmanagement.data.share.TransactionShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet that previews the generated transaction image, lets the user pick one of the
 * 12 backgrounds, then either save it to the gallery or share it to any app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionShareSheet(
    transaction: TransactionEntity,
    categoryTitle: String,
    categoryEmoji: String,
    currencyUnit: String,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val content = remember(transaction, categoryTitle, categoryEmoji, currencyUnit, isPersian) {
        TransactionShareFormatter.build(transaction, categoryTitle, categoryEmoji, currencyUnit, isPersian)
    }

    // Different transactions open with different artwork; the user can change it freely.
    var selectedIndex by rememberSaveable(transaction.id) {
        mutableStateOf(Math.floorMod(transaction.id.hashCode(), TransactionShareCardRenderer.BACKGROUND_COUNT))
    }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }
    var renderFailed by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(content, selectedIndex) {
        isRendering = true
        renderFailed = false
        val result = withContext(Dispatchers.Default) {
            try {
                TransactionShareCardRenderer.render(context, content, selectedIndex)
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
        if (result != null) preview = result else renderFailed = true
        isRendering = false
    }

    val previewImage: ImageBitmap? = remember(preview) { preview?.asImageBitmap() }

    fun toast(fa: String, en: String) {
        Toast.makeText(context, if (isPersian) fa else en, Toast.LENGTH_SHORT).show()
    }

    fun performSave() {
        val bitmap = preview ?: return
        scope.launch {
            isBusy = true
            val uri = TransactionShareManager.saveToGallery(context, bitmap)
            isBusy = false
            if (uri != null) {
                toast("تصویر در گالری ذخیره شد", "Image saved to your gallery")
            } else {
                toast("ذخیره‌سازی تصویر ناموفق بود", "Couldn't save the image")
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) performSave()
        else toast("برای ذخیره تصویر به دسترسی حافظه نیاز است", "Storage permission is required to save the image")
    }

    val canAct = previewImage != null && !isRendering && !isBusy

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPersian) "اشتراک‌گذاری تراکنش" else "Share Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Live preview of the exact image that will be saved / shared
            val previewShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(TransactionShareCardRenderer.ASPECT_RATIO)
                    .clip(previewShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), previewShape),
                contentAlignment = Alignment.Center
            ) {
                if (previewImage != null) {
                    Image(
                        bitmap = previewImage,
                        contentDescription = if (isPersian) "پیش‌نمایش تصویر تراکنش" else "Transaction image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isRendering) 0.45f else 1f)
                    )
                }
                if (isRendering) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                } else if (renderFailed && previewImage == null) {
                    Text(
                        text = if (isPersian) "ساخت تصویر ناموفق بود" else "Couldn't build the image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text(
                text = if (isPersian) "پس‌زمینه" else "Background",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(TransactionShareCardRenderer.BACKGROUND_COUNT) { index ->
                    BackgroundThumbnail(
                        index = index,
                        selected = index == selectedIndex,
                        isPersian = isPersian,
                        onClick = { selectedIndex = index }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val needsPermission = TransactionShareManager.needsLegacyStoragePermission() &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            performSave()
                        }
                    },
                    enabled = canAct,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "ذخیره" else "Save",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val bitmap = preview ?: return@Button
                        scope.launch {
                            isBusy = true
                            val opened = TransactionShareManager.shareImage(
                                context,
                                bitmap,
                                if (isPersian) "اشتراک‌گذاری تراکنش" else "Share transaction"
                            )
                            isBusy = false
                            if (!opened) toast("اشتراک‌گذاری ناموفق بود", "Couldn't share the image")
                        }
                    },
                    enabled = canAct,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "اشتراک‌گذاری" else "Share",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundThumbnail(
    index: Int,
    selected: Boolean,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(null, index, isPersian) {
        value = withContext(Dispatchers.Default) {
            try {
                TransactionShareCardRenderer.renderBackgroundThumbnail(context, index, 144, 180, isPersian).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(75.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(if (selected) 2.5.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        thumbnail?.let {
            Image(
                bitmap = it,
                contentDescription = (if (isPersian) "پس‌زمینه " else "Background ") +
                        TransactionShareCardRenderer.backgroundLabel(index, isPersian),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}