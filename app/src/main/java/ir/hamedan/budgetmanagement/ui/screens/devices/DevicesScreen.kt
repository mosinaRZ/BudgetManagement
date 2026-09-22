package ir.hamedan.budgetmanagement.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.hamedan.budgetmanagement.di.appViewModel
import ir.hamedan.budgetmanagement.ui.theme.isPersianLocale
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(onBack: () -> Unit) {
    val vm: DevicesViewModel = appViewModel()
    val devices by vm.devices.collectAsState()
    val isPersian = isPersianLocale()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPersian) "دستگاه‌های حساب" else "Account Devices") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Default.Devices, null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            items(devices, key = { it.id }) { device ->
                val current = device.id == vm.currentDeviceId
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, null)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(if (current) { if (isPersian) "این دستگاه" else "This device" } else { if (isPersian) "دستگاه متصل" else "Connected device" }, style = MaterialTheme.typography.titleMedium)
                            Text(if (isPersian) "آخرین فعالیت: ${formatDate(device.lastSeenAt)}" else "Last activity: ${formatDate(device.lastSeenAt)}", style = MaterialTheme.typography.bodySmall)
                            Text(if (isPersian) "شناسه: ${device.id.take(12)}…" else "ID: ${device.id.take(12)}…", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!current) {
                            IconButton(onClick = { vm.revoke(device.id) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = if (isPersian) "لغو دستگاه" else "Revoke device")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))