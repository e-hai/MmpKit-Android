package com.kit.mmp.adjust

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustDeeplink
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel
import com.kit.mmp.MmpClient
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.attribution.AttributionListener
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.deeplink.DeepLinkListener
import com.kit.mmp.deeplink.DeepLinkResult
import com.kit.mmp.event.MmpEvent
import com.kit.mmp.event.MmpRevenue
import com.kit.mmp.log.MmpLog

/**
 * Adjust [MmpClient] adapter.
 *
 * Note: [MmpEvent.name] / [MmpRevenue.eventName] are treated as Adjust **event tokens**.
 */
class AdjustClient : MmpClient {
    @Volatile
    private var attributionListener: AttributionListener? = null

    @Volatile
    private var deepLinkListener: DeepLinkListener? = null

    @Volatile
    private var cachedAttribution: AttributionData? = null

    override fun initialize(context: Context, config: MmpConfig) {
        val environment = if (config.isDebug) {
            AdjustConfig.ENVIRONMENT_SANDBOX
        } else {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        }
        MmpLog.i(
            "Adjust initialize: token=${MmpLog.maskToken(config.appToken)}, " +
                "env=$environment, isDebug=${config.isDebug}, appId=${config.appId}",
            TAG,
        )
        val adjustConfig = AdjustConfig(context.applicationContext, config.appToken, environment)
        adjustConfig.setLogLevel(if (config.isDebug) LogLevel.VERBOSE else LogLevel.WARN)
        adjustConfig.setOnAttributionChangedListener { attribution ->
            val data = attribution.toAttributionData()
            cachedAttribution = data
            MmpLog.i("Adjust onAttributionChanged: $data", TAG)
            attributionListener?.onAttribution(data)
        }
        adjustConfig.setOnDeferredDeeplinkResponseListener { uri: Uri? ->
            if (uri == null) {
                MmpLog.d("Adjust deferred deeplink null", TAG)
                return@setOnDeferredDeeplinkResponseListener false
            }
            val mapped = uri.toDeepLinkResult(isDeferred = true)
            MmpLog.i("Adjust deferred deeplink: $mapped", TAG)
            deepLinkListener?.onDeepLink(mapped)
            // App handles navigation via DeepLinkListener.
            false
        }
        Adjust.initSdk(adjustConfig)
        MmpLog.i("Adjust SDK initialized", TAG)
    }

    override fun setAttributionListener(listener: AttributionListener?) {
        MmpLog.d("Adjust setAttributionListener(listener=${listener != null})", TAG)
        attributionListener = listener
        cachedAttribution?.let {
            MmpLog.d("Adjust delivering cached attribution to new listener: $it", TAG)
            listener?.onAttribution(it)
        }
    }

    override fun setDeepLinkListener(listener: DeepLinkListener?) {
        MmpLog.d("Adjust setDeepLinkListener(listener=${listener != null})", TAG)
        deepLinkListener = listener
    }

    override fun handleDeepLink(context: Context, intent: Intent?) {
        val uri = intent?.data ?: run {
            MmpLog.d("Adjust handleDeepLink skipped: intent data null", TAG)
            return
        }
        MmpLog.d("Adjust handleDeepLink uri=$uri", TAG)
        Adjust.processAndResolveDeeplink(AdjustDeeplink(uri), context.applicationContext) { resolvedLink ->
            val resolvedUri = resolvedLink?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            if (resolvedUri == null) {
                MmpLog.w("Adjust direct deeplink unresolved or empty", TAG)
                return@processAndResolveDeeplink
            }
            val mapped = resolvedUri.toDeepLinkResult(isDeferred = false)
            MmpLog.i("Adjust direct deeplink resolved: $mapped", TAG)
            deepLinkListener?.onDeepLink(mapped)
        }
    }

    override fun trackEvent(event: MmpEvent) {
        MmpLog.d("Adjust trackEvent token=${event.name}, params=${event.params}", TAG)
        val adjustEvent = AdjustEvent(event.name)
        event.params.forEach { (key, value) ->
            value?.let { adjustEvent.addCallbackParameter(key, it.toString()) }
        }
        Adjust.trackEvent(adjustEvent)
    }

    override fun trackRevenue(revenue: MmpRevenue) {
        MmpLog.d(
            "Adjust trackRevenue token=${revenue.eventName}, revenue=${revenue.revenue}, " +
                "currency=${revenue.currency}, params=${revenue.params}",
            TAG,
        )
        val adjustEvent = AdjustEvent(revenue.eventName)
        adjustEvent.setRevenue(revenue.revenue, revenue.currency)
        revenue.params.forEach { (key, value) ->
            value?.let { adjustEvent.addCallbackParameter(key, it.toString()) }
        }
        Adjust.trackEvent(adjustEvent)
    }

    override fun getAttribution(callback: (AttributionData?) -> Unit) {
        val cached = cachedAttribution
        if (cached != null) {
            MmpLog.d("Adjust getAttribution from cache: $cached", TAG)
            callback(cached)
            return
        }
        MmpLog.d("Adjust getAttribution fetching from SDK", TAG)
        Adjust.getAttribution { attribution ->
            val data = attribution?.toAttributionData()
            if (data != null) {
                cachedAttribution = data
            }
            MmpLog.d("Adjust getAttribution result=$data", TAG)
            callback(data)
        }
    }

    override fun peekAttribution(): AttributionData? = cachedAttribution

    /**
     * 本地测试 / 模拟方法：手动注入归因数据，无需连接 Adjust 真实后台。
     */
    fun simulateAttributionData(data: AttributionData) {
        cachedAttribution = data
        MmpLog.i("Adjust [Simulated] attribution: $data", TAG)
        attributionListener?.onAttribution(data)
    }

    /**
     * 本地测试 / 模拟方法：手动触发深度链接回调。
     */
    fun simulateDeepLink(
        deepLinkValue: String?,
        clickEvent: Map<String, Any?> = emptyMap(),
        isDeferred: Boolean = false,
    ) {
        val mapped = DeepLinkResult(
            deepLinkValue = deepLinkValue,
            clickEvent = clickEvent,
            isDeferred = isDeferred,
        )
        MmpLog.i("Adjust [Simulated] deep link: $mapped", TAG)
        deepLinkListener?.onDeepLink(mapped)
    }

    private companion object {
        const val TAG = "MmpKit/Adjust"

        fun Uri.toDeepLinkResult(isDeferred: Boolean): DeepLinkResult = DeepLinkResult(
            deepLinkValue = toString(),
            clickEvent = queryParameterNames.associateWith { getQueryParameter(it) },
            isDeferred = isDeferred,
        )

        fun AdjustAttribution.toAttributionData(): AttributionData {
            val raw = linkedMapOf<String, Any?>(
                "trackerToken" to trackerToken,
                "trackerName" to trackerName,
                "network" to network,
                "campaign" to campaign,
                "adgroup" to adgroup,
                "creative" to creative,
                "clickLabel" to clickLabel,
                "costType" to costType,
                "costAmount" to costAmount,
                "costCurrency" to costCurrency,
            )
            return AttributionData(
                network = network,
                campaign = campaign,
                adGroup = adgroup,
                creative = creative,
                clickLabel = clickLabel,
                isOrganic = network.isNullOrBlank() ||
                    network.equals("Organic", ignoreCase = true),
                raw = raw,
            )
        }
    }
}
