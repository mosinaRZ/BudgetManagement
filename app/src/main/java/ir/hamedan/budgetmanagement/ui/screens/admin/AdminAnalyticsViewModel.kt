package ir.hamedan.budgetmanagement.ui.screens.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.hamedan.budgetmanagement.data.network.AdminApi
import ir.hamedan.budgetmanagement.data.network.ApiException
import ir.hamedan.budgetmanagement.platform.locale.LocaleHelper
import ir.hamedan.budgetmanagement.utils.ErrorMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Filters shared by every chart and the event feed, so the whole page always tells one story. */
data class AnalyticsFilters(
    val days: Int = 30,
    val severity: String = "",
    val types: Set<String> = emptySet(),
    val platform: String = "",
    val userId: String = "",
    val userLabel: String = ""
) {
    val activeCount: Int
        get() = listOf(severity.isNotEmpty(), types.isNotEmpty(), platform.isNotEmpty(), userId.isNotEmpty()).count { it }
}

data class AnalyticsUiState(
    val filters: AnalyticsFilters = AnalyticsFilters(),
    val data: AdminApi.Analytics? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val forbidden: Boolean = false,
    val events: List<AdminApi.AdminEvent> = emptyList(),
    val eventsTotal: Long = 0L,
    val eventsLoadingMore: Boolean = false
) {
    val canLoadMoreEvents: Boolean get() = events.size < eventsTotal
}

class AdminAnalyticsViewModel(
    private val adminApi: AdminApi,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private val isPersian: Boolean get() = LocaleHelper.getLanguage(context) == "fa"

    init { reload() }

    fun setDays(days: Int) = change { it.copy(days = days) }
    fun setSeverity(severity: String) = change { it.copy(severity = severity) }
    fun setPlatform(platform: String) = change { it.copy(platform = platform) }
    fun setTypes(types: Set<String>) = change { it.copy(types = types) }
    fun filterUser(userId: String, label: String) = change { it.copy(userId = userId, userLabel = label) }
    fun clearUser() = change { it.copy(userId = "", userLabel = "") }
    fun clearFilters() = change { AnalyticsFilters(days = it.days) }

    private fun change(f: (AnalyticsFilters) -> AnalyticsFilters) {
        _state.update { it.copy(filters = f(it.filters)) }
        reload()
    }

    fun reload(silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (!silent) _state.update { it.copy(isLoading = true, error = null) }
            val f = _state.value.filters
            try {
                val (analytics, events) = withContext(Dispatchers.IO) {
                    val a = adminApi.analytics(
                        AdminApi.AnalyticsQuery(days = f.days, severity = f.severity, types = f.types, platform = f.platform, userId = f.userId)
                    )
                    val e = adminApi.events(
                        AdminApi.EventQuery(severity = f.severity, types = f.types, userId = f.userId, days = f.days, limit = 30)
                    )
                    a to e
                }
                _state.update {
                    it.copy(data = analytics, events = events.events, eventsTotal = events.total, isLoading = false, error = null, forbidden = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        forbidden = (e as? ApiException)?.statusCode == 403,
                        error = if (silent && it.data != null) it.error else ErrorMessages.of(e, isPersian)
                    )
                }
            }
        }
    }

    fun loadMoreEvents() {
        val s = _state.value
        if (s.eventsLoadingMore || !s.canLoadMoreEvents) return
        viewModelScope.launch {
            _state.update { it.copy(eventsLoadingMore = true) }
            val f = s.filters
            try {
                val page = withContext(Dispatchers.IO) {
                    adminApi.events(AdminApi.EventQuery(severity = f.severity, types = f.types, userId = f.userId, days = f.days, limit = 30, offset = s.events.size))
                }
                _state.update { cur ->
                    val known = cur.events.mapTo(HashSet()) { it.id }
                    cur.copy(events = cur.events + page.events.filter { it.id !in known }, eventsTotal = page.total, eventsLoadingMore = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(eventsLoadingMore = false, error = ErrorMessages.of(e, isPersian)) }
            }
        }
    }
}