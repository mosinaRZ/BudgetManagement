package ir.hamedan.budgetmanagement.ui.screens.devices

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.data.network.DeviceApi
import ir.hamedan.budgetmanagement.data.security.DeviceIdentityStore
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevicesViewModel(
    private val deviceApi: DeviceApi,
    private val deviceIdentityStore: DeviceIdentityStore,
    private val context: Context
) : ViewModel() {
    private val _devices = MutableStateFlow<List<DeviceApi.Device>>(emptyList())
    val devices: StateFlow<List<DeviceApi.Device>> = _devices.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Id of the device whose removal is in flight (drives the per-card progress state). */
    private val _revokingId = MutableStateFlow<String?>(null)
    val revokingId: StateFlow<String?> = _revokingId.asStateFlow()

    /** One-shot confirmation text; the screen shows it and calls [consumeNotice]. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    val currentDeviceId: String = deviceIdentityStore.getOrCreate()

    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"

    init { refresh() }

    fun refresh() {
        if (_isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            runCatching { deviceApi.list() }
                .onSuccess { _devices.value = sorted(it); _error.value = null }
                .onFailure { _error.value = message(it) }
            _isLoading.value = false
        }
    }

    fun revoke(deviceId: String) {
        // The server cannot tell which device is "this one" from the access token, so the
        // client refuses to remove itself (that would be a sign-out, handled in Settings).
        if (deviceId == currentDeviceId || _revokingId.value != null) return
        // The primary (first) device can only be removed from itself. The server enforces this
        // too; checking here just avoids a pointless request and a confusing error.
        if (_devices.value.any { it.id == deviceId && it.isPrimary }) {
            _error.value = if (isPersian) "دستگاه اصلی فقط از خود همان دستگاه قابل حذف است."
            else "The primary device can only be removed from the primary device itself."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _revokingId.value = deviceId
            runCatching { deviceApi.revoke(deviceId) }
                .onSuccess {
                    _devices.value = _devices.value.filterNot { it.id == deviceId }
                    _error.value = null
                    _notice.value = if (isPersian) "دستگاه از حساب حذف شد." else "Device removed from your account."
                }
                .onFailure { failure ->
                    if ((failure as? ApiException)?.statusCode == 404) {
                        // Already gone (removed from another device): just resync the list.
                        _devices.value = _devices.value.filterNot { it.id == deviceId }
                    } else {
                        _error.value = message(failure)
                    }
                }
            _revokingId.value = null
        }
    }

    fun consumeNotice() { _notice.value = null }

    private fun sorted(list: List<DeviceApi.Device>): List<DeviceApi.Device> =
        list.sortedWith(
            compareByDescending<DeviceApi.Device> { it.id == currentDeviceId }
                .thenByDescending { it.isPrimary }
                .thenByDescending { it.lastSeenAt }
        )

    private fun message(error: Throwable): String =
        (error as? ApiException)?.userMessage(isPersian)
            ?: if (isPersian) "عملیات ناموفق بود. دوباره تلاش کنید." else "The operation failed. Please try again."
}