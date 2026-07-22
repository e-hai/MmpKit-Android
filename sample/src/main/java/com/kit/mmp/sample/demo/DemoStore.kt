package com.kit.mmp.sample.demo

import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.deeplink.DeepLinkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DemoUiState(
    val platform: String = "AppsFlyer",
    val logEnabled: Boolean = true,
    val lastAttribution: AttributionData? = null,
    val lastDeepLink: DeepLinkResult? = null,
    val logs: List<String> = emptyList(),
)

/**
 * In-memory bridge between [com.kit.mmp.sample.SampleApp] callbacks and the demo UI.
 */
object DemoStore {
    private const val MAX_LOGS = 40
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _state = MutableStateFlow(DemoUiState())
    val state: StateFlow<DemoUiState> = _state.asStateFlow()

    fun setPlatform(platform: String) {
        _state.update { it.copy(platform = platform) }
    }

    fun setLogEnabled(enabled: Boolean) {
        _state.update { it.copy(logEnabled = enabled) }
        append("Log ${if (enabled) "enabled" else "disabled"}")
    }

    fun onAttribution(data: AttributionData) {
        _state.update { it.copy(lastAttribution = data) }
        append("Attribution: network=${data.network}, campaign=${data.campaign}, organic=${data.isOrganic}")
    }

    fun onDeepLink(result: DeepLinkResult) {
        _state.update { it.copy(lastDeepLink = result) }
        append("DeepLink: value=${result.deepLinkValue}, deferred=${result.isDeferred}")
    }

    fun append(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        _state.update { current ->
            val next = (listOf(line) + current.logs).take(MAX_LOGS)
            current.copy(logs = next)
        }
    }
}
