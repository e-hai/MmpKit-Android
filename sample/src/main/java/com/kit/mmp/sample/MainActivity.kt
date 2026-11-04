package com.kit.mmp.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kit.mmp.Mmp
import com.kit.mmp.adjust.AdjustClient
import com.kit.mmp.appsflyer.AppsFlyerClient
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.deeplink.DeepLinkResult
import com.kit.mmp.event.MmpRevenue
import com.kit.mmp.sample.demo.DemoStore
import com.kit.mmp.sample.demo.DemoUiState
import com.kit.mmp.sample.ui.theme.MmpKitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MmpKitTheme {
                val state by DemoStore.state.collectAsState()
                DemoScreen(
                    state = state,
                    onTrackEvent = {
                        Mmp.trackEvent(
                            "level_complete",
                            mapOf("level" to 3, "score" to 1200),
                        )
                        DemoStore.append("trackEvent(level_complete, level=3, score=1200)")
                    },
                    onTrackRevenue = {
                        Mmp.trackRevenue(
                            MmpRevenue(
                                revenue = 9.99,
                                currency = "USD",
                                params = mapOf("sku" to "coin_pack_s"),
                            ),
                        )
                        DemoStore.append("trackRevenue(9.99 USD, sku=coin_pack_s)")
                    },
                    onGetAttribution = {
                        DemoStore.append(
                            "sync peek: isOrganic=${Mmp.isOrganic}, " +
                                "network=${Mmp.network}, campaign=${Mmp.campaign}",
                        )
                        val cached = Mmp.attribution
                        if (cached != null) {
                            DemoStore.onAttribution(cached)
                        }
                        Mmp.getAttribution { data ->
                            if (data != null) {
                                DemoStore.onAttribution(data)
                            } else {
                                DemoStore.append("getAttribution → null (not ready yet)")
                            }
                        }
                    },
                    onToggleLog = {
                        val next = !Mmp.isLogEnabled()
                        Mmp.setLogEnabled(next)
                        DemoStore.setLogEnabled(next)
                    },
                    onSimulateNonOrganic = {
                        val client = SampleApp.currentClient
                        if (client is AppsFlyerClient) {
                            client.simulateConversionData(
                                mapOf(
                                    "af_status" to "Non-organic",
                                    "media_source" to "googleadwords_int",
                                    "campaign" to "summer_sale_2026",
                                    "af_adset" to "group_android",
                                    "af_ad" to "banner_300x250",
                                ),
                            )
                        } else if (client is AdjustClient) {
                            client.simulateAttributionData(
                                AttributionData(
                                    network = "googleadwords_int",
                                    campaign = "summer_sale_2026",
                                    adGroup = "group_android",
                                    creative = "banner_300x250",
                                    clickLabel = null,
                                    isOrganic = false,
                                ),
                            )
                        }
                        Mmp.attribution?.let { DemoStore.onAttribution(it) }
                        DemoStore.append("Simulated: Non-organic attribution injected")
                    },
                    onSimulateOrganic = {
                        val client = SampleApp.currentClient
                        if (client is AppsFlyerClient) {
                            client.simulateConversionData(
                                mapOf(
                                    "af_status" to "Organic",
                                ),
                            )
                        } else if (client is AdjustClient) {
                            client.simulateAttributionData(
                                AttributionData(
                                    network = "Organic",
                                    campaign = null,
                                    adGroup = null,
                                    creative = null,
                                    clickLabel = null,
                                    isOrganic = true,
                                ),
                            )
                        }
                        Mmp.attribution?.let { DemoStore.onAttribution(it) }
                        DemoStore.append("Simulated: Organic attribution injected")
                    },
                    onSimulateDeepLink = {
                        val client = SampleApp.currentClient
                        if (client is AppsFlyerClient) {
                            client.simulateDeepLink(
                                deepLinkValue = "myapp://promo/spring_sale",
                                clickEvent = mapOf("campaign" to "spring_sale", "discount" to "50%"),
                                isDeferred = true,
                            )
                        } else if (client is AdjustClient) {
                            client.simulateDeepLink(
                                deepLinkValue = "myapp://promo/spring_sale",
                                clickEvent = mapOf("campaign" to "spring_sale", "discount" to "50%"),
                                isDeferred = true,
                            )
                        }
                        DemoStore.append("Simulated: DeepLink event injected")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DemoScreen(
    state: DemoUiState,
    onTrackEvent: () -> Unit = {},
    onTrackRevenue: () -> Unit = {},
    onGetAttribution: () -> Unit = {},
    onToggleLog: () -> Unit = {},
    onSimulateNonOrganic: () -> Unit = {},
    onSimulateOrganic: () -> Unit = {},
    onSimulateDeepLink: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MmpKit Demo")
                        Text(
                            text = "Unified MMP playground",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatusSection(state = state)
            }
            item {
                ActionsSection(
                    logEnabled = state.logEnabled,
                    onTrackEvent = onTrackEvent,
                    onTrackRevenue = onTrackRevenue,
                    onGetAttribution = onGetAttribution,
                    onToggleLog = onToggleLog,
                    onSimulateNonOrganic = onSimulateNonOrganic,
                    onSimulateOrganic = onSimulateOrganic,
                    onSimulateDeepLink = onSimulateDeepLink,
                )
            }
            item {
                InfoCard(
                    title = "Last attribution",
                    body = state.lastAttribution.toDisplayText(),
                )
            }
            item {
                InfoCard(
                    title = "Last deep link",
                    body = state.lastDeepLink.toDisplayText(),
                )
            }
            item {
                Text(
                    text = "Activity log",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.logs.isEmpty()) {
                item {
                    Text(
                        text = "No activity yet. Tap an action above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.logs) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSection(state: DemoUiState) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Runtime", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(state.platform) },
                    enabled = false,
                )
                FilterChip(
                    selected = state.logEnabled,
                    onClick = {},
                    label = { Text(if (state.logEnabled) "Log ON" else "Log OFF") },
                    enabled = false,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Replace AppsFlyerClient with AdjustClient in SampleApp to switch vendors.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionsSection(
    logEnabled: Boolean,
    onTrackEvent: () -> Unit,
    onTrackRevenue: () -> Unit,
    onGetAttribution: () -> Unit,
    onToggleLog: () -> Unit,
    onSimulateNonOrganic: () -> Unit,
    onSimulateOrganic: () -> Unit,
    onSimulateDeepLink: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Actions", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onTrackEvent,
                modifier = Modifier.weight(1f),
            ) { Text("Track Event") }
            Button(
                onClick = onTrackRevenue,
                modifier = Modifier.weight(1f),
            ) { Text("Track Revenue") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onGetAttribution,
                modifier = Modifier.weight(1f),
            ) { Text("Get Attribution") }
            OutlinedButton(
                onClick = onToggleLog,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (logEnabled) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Text(if (logEnabled) "Disable Log" else "Enable Log")
            }
        }
        Text("Local Testing / Mock Simulation", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSimulateNonOrganic,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text("Mock Non-Organic", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = onSimulateOrganic,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Text("Mock Organic", style = MaterialTheme.typography.labelSmall)
            }
        }
        Button(
            onClick = onSimulateDeepLink,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text("Mock DeepLink (myapp://promo/spring_sale)")
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun AttributionData?.toDisplayText(): String {
    if (this == null) return "Waiting for conversion / attribution callback…"
    return buildString {
        appendLine("network: ${network ?: "-"}")
        appendLine("campaign: ${campaign ?: "-"}")
        appendLine("adGroup: ${adGroup ?: "-"}")
        appendLine("creative: ${creative ?: "-"}")
        appendLine("clickLabel: ${clickLabel ?: "-"}")
        append("organic: $isOrganic")
    }
}

private fun DeepLinkResult?.toDisplayText(): String {
    if (this == null) return "No deep link received yet."
    return buildString {
        appendLine("value: ${deepLinkValue ?: "-"}")
        appendLine("deferred: $isDeferred")
        append("clickEvent: ${if (clickEvent.isEmpty()) "-" else clickEvent}")
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoScreenPreview() {
    MmpKitTheme {
        DemoScreen(
            state = DemoUiState(
                platform = "AppsFlyer",
                logEnabled = true,
                logs = listOf("12:00:01  Mmp initialized with AppsFlyerClient"),
            ),
        )
    }
}
