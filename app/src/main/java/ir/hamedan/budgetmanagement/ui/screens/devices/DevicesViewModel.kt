package ir.hamedan.budgetmanagement.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevicesViewModel(
    private val deviceApi: DeviceApi,
    private val deviceIdentityStore: DeviceIdentityStore
) : ViewModel() {
    private val _devices = MutableStateFlow<List<DeviceApi.Device>>(emptyList())
    val devices: StateFlow<List<DeviceApi.Device>> = _devices.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val currentDeviceId: String = deviceIdentityStore.getOrCreate()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { deviceApi.list() }
                .onSuccess { _devices.value = it; _error.value = null }
                .onFailure { _error.value = it.message }
        }
    }

    fun revoke(deviceId: String) {
        if (deviceId == currentDeviceId) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { deviceApi.revoke(deviceId) }
                .onSuccess { refresh() }
                .onFailure { _error.value = it.message }
        }
    }
}