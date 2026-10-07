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

/**
 * Only the account's primary (first) device may remove other devices; every other device is
 * view-only. When the server did not flag any primary (an older server), stay permissive and let
 * the server decide, which also enforces the rule.
 */
internal fun canRemoveOtherDevices(devices: List<DeviceApi.Device>, currentDeviceId: String): Boolean =
    devices.none { it.isPrimary } || devices.any { it.id == currentDeviceId && it.isPrimary }

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
        // Removing this very device would be a sign-out, which is handled in Settings.
        if (deviceId == currentDeviceId || _revokingId.value != null) return
        // Only the primary device may remove others. The server enforces this too; checking here
        // avoids a pointless request and shows a clear message.
        if (!canRemoveOtherDevices(_devices.value, currentDeviceId)) {
            _error.value = if (isPersian) "فقط دستگاه اصلی می‌تواند دستگاه‌های دیگر را حذف کند."
            else "Only the primary device can remove other devices."
            return
        }
        // The primary (first) device can only be removed from itself. The server enforces this
        // too; checking here just avoids a pointless request and a confusing error.
        if (_devices.value.any { it.id == deviceId && it.isPrimary }) {
            _error.value = if (isPersian) "دستگاه اصلی فقط از خود همان دستگاه قابل حذف است."
            else "The primary device can only be removed from the primary device itself."
            return
        }
        val removedName = _devices.value.firstOrNull { it.id == deviceId }
            ?.let { it.name.ifBlank { it.model } }
            .orEmpty()
        viewModelScope.launch(Dispatchers.IO) {
            _revokingId.value = deviceId
            val result = runCatching { deviceApi.revoke(deviceId) }

            // The server is the source of truth. Never edit the list locally and assume it worked:
            // always reload it, so the screen can only show a device as gone when it really is.
            val reloaded = runCatching { deviceApi.list() }.getOrNull()
            if (reloaded != null) _devices.value = sorted(reloaded)
            val stillListed = reloaded?.any { it.id == deviceId } ?: false

            val failure = result.exceptionOrNull()
            when {
                // Removed on the server and confirmed by the fresh list.
                failure == null && reloaded != null && !stillListed -> {
                    _error.value = null
                    _notice.value = when {
                        removedName.isBlank() && isPersian -> "دستگاه از حساب حذف شد."
                        removedName.isBlank() -> "Device removed from your account."
                        isPersian -> "«$removedName» از حساب حذف شد و دسترسی‌اش قطع شد."
                        else -> "\"$removedName\" was removed and no longer has access."
                    }
                }
                // The server said "done" but the device is still on the account.
                failure == null && stillListed -> {
                    _error.value = if (isPersian) "حذف دستگاه انجام نشد؛ دستگاه هنوز در حساب است."
                    else "The device could not be removed; it is still on your account."
                }
                // Success but the list could not be reloaded: report the request, not a guess.
                failure == null -> {
                    _error.value = null
                    _notice.value = if (isPersian) "درخواست حذف ارسال شد. برای اطمینان لیست را بازخوانی کنید."
                    else "Removal requested. Refresh the list to confirm."
                }
                (failure as? ApiException)?.statusCode == 404 && reloaded != null && !stillListed -> {
                    _error.value = null // it was already gone (removed from elsewhere)
                }
                else -> _error.value = message(failure)
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