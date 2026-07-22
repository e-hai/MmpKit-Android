package com.kit.mmp.appsflyer

import android.content.Context
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.kit.mmp.MmpClient
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.attribution.AttributionListener
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.deeplink.DeepLinkListener as MmpDeepLinkListener
import com.kit.mmp.deeplink.DeepLinkResult as MmpDeepLinkResult
import com.kit.mmp.event.MmpEvent
import com.kit.mmp.event.MmpRevenue
import com.kit.mmp.log.MmpLog

/**
 * AppsFlyer [MmpClient] adapter.
 */
class AppsFlyerClient : MmpClient {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var attributionListener: AttributionListener? = null

    @Volatile
    private var deepLinkListener: MmpDeepLinkListener? = null

    @Volatile
    private var cachedAttribution: AttributionData? = null

    override fun initialize(context: Context, config: MmpConfig) {
        val appContext = context.applicationContext
        this.appContext = appContext

        MmpLog.i(
            "AppsFlyer initialize: token=${MmpLog.maskToken(config.appToken)}, " +
                "isDebug=${config.isDebug}, appId=${config.appId}",
            TAG,
        )
        val af = AppsFlyerLib.getInstance()
        af.setDebugLog(config.isDebug)
        af.init(config.appToken, conversionListener, appContext)
        af.subscribeForDeepLink(udlListener)
        af.start(appContext)
        MmpLog.i("AppsFlyer started", TAG)
    }

    override fun setAttributionListener(listener: AttributionListener?) {
        MmpLog.d("AppsFlyer setAttributionListener(listener=${listener != null})", TAG)
        attributionListener = listener
        cachedAttribution?.let {
            MmpLog.d("AppsFlyer delivering cached attribution to new listener: $it", TAG)
            listener?.onAttribution(it)
        }
    }

    override fun setDeepLinkListener(listener: MmpDeepLinkListener?) {
        MmpLog.d("AppsFlyer setDeepLinkListener(listener=${listener != null})", TAG)
        deepLinkListener = listener
    }

    override fun trackEvent(event: MmpEvent) {
        val context = appContext
        if (context == null) {
            MmpLog.w("AppsFlyer trackEvent skipped: not initialized", TAG)
            return
        }
        val params = event.params.toAfParams()
        MmpLog.d("AppsFlyer logEvent name=${event.name}, params=$params", TAG)
        AppsFlyerLib.getInstance().logEvent(context, event.name, params)
    }

    override fun trackRevenue(revenue: MmpRevenue) {
        val context = appContext
        if (context == null) {
            MmpLog.w("AppsFlyer trackRevenue skipped: not initialized", TAG)
            return
        }
        val params = revenue.params.toAfParams().toMutableMap()
        params[AFInAppEventParameterName.REVENUE] = revenue.revenue
        params[AFInAppEventParameterName.CURRENCY] = revenue.currency
        MmpLog.d(
            "AppsFlyer logRevenue name=${revenue.eventName}, " +
                "revenue=${revenue.revenue}, currency=${revenue.currency}, params=$params",
            TAG,
        )
        AppsFlyerLib.getInstance().logEvent(context, revenue.eventName, params)
    }

    override fun getAttribution(callback: (AttributionData?) -> Unit) {
        MmpLog.d("AppsFlyer getAttribution cached=$cachedAttribution", TAG)
        callback(cachedAttribution)
    }

    override fun peekAttribution(): AttributionData? = cachedAttribution

    private val conversionListener = object : AppsFlyerConversionListener {
        override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>?) {
            MmpLog.d("AppsFlyer onConversionDataSuccess raw=$conversionData", TAG)
            val data = conversionData?.toAttributionData() ?: run {
                MmpLog.w("AppsFlyer conversion data empty", TAG)
                return
            }
            cachedAttribution = data
            MmpLog.i("AppsFlyer attribution: $data", TAG)
            attributionListener?.onAttribution(data)
        }

        override fun onConversionDataFail(errorMessage: String?) {
            MmpLog.w("AppsFlyer onConversionDataFail: $errorMessage", TAG)
        }

        override fun onAppOpenAttribution(attributionData: MutableMap<String, String>?) {
            MmpLog.d("AppsFlyer onAppOpenAttribution (legacy): $attributionData", TAG)
        }

        override fun onAttributionFailure(errorMessage: String?) {
            MmpLog.w("AppsFlyer onAttributionFailure: $errorMessage", TAG)
        }
    }

    private val udlListener = DeepLinkListener { result ->
        when (result.status) {
            DeepLinkResult.Status.FOUND -> {
                val deepLink = result.deepLink ?: run {
                    MmpLog.w("AppsFlyer deep link FOUND but payload null", TAG)
                    return@DeepLinkListener
                }
                val mapped = MmpDeepLinkResult(
                    deepLinkValue = deepLink.deepLinkValue,
                    clickEvent = deepLink.clickEvent.toMapOrEmpty(),
                    isDeferred = deepLink.isDeferred == true,
                )
                MmpLog.i("AppsFlyer deep link: $mapped", TAG)
                deepLinkListener?.onDeepLink(mapped)
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                MmpLog.d("AppsFlyer deep link not found", TAG)
            }
            DeepLinkResult.Status.ERROR -> {
                MmpLog.w("AppsFlyer deep link error: ${result.error}", TAG)
            }
        }
    }

    private companion object {
        const val TAG = "MmpKit/AppsFlyer"

        fun Map<String, Any?>.toAfParams(): Map<String, Any> =
            mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

        fun org.json.JSONObject?.toMapOrEmpty(): Map<String, Any?> {
            if (this == null) return emptyMap()
            val result = linkedMapOf<String, Any?>()
            keys().forEach { key -> result[key] = opt(key) }
            return result
        }

        fun Map<String, Any>.toAttributionData(): AttributionData {
            val status = this["af_status"]?.toString()
            return AttributionData(
                network = this["media_source"]?.toString(),
                campaign = this["campaign"]?.toString(),
                adGroup = this["adgroup"]?.toString()
                    ?: this["af_adset"]?.toString(),
                creative = this["af_ad"]?.toString()
                    ?: this["adgroup"]?.toString(),
                clickLabel = this["af_c_id"]?.toString(),
                isOrganic = status.equals("Organic", ignoreCase = true),
                raw = this,
            )
        }
    }
}
