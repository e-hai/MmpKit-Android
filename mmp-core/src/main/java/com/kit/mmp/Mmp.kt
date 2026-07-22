package com.kit.mmp

import android.content.Context
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.attribution.AttributionListener
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.deeplink.DeepLinkListener
import com.kit.mmp.event.MmpEvent
import com.kit.mmp.event.MmpRevenue
import com.kit.mmp.log.MmpLog

/**
 * Static facade that delegates to a single [MmpClient] implementation.
 * Methods are no-ops (with a debug log) until [init] is called.
 *
 * Attribution is cached after the first successful callback / fetch.
 * Prefer sync accessors ([attribution], [isOrganic], [network], …) for common checks;
 * use [setAttributionListener] when you need to react as soon as data arrives.
 */
object Mmp {
    @Volatile
    private var client: MmpClient? = null

    @Volatile
    private var cachedAttribution: AttributionData? = null

    @Volatile
    private var userAttributionListener: AttributionListener? = null

    private val lock = Any()

    /** Latest cached attribution, or null if not ready yet. */
    val attribution: AttributionData?
        get() = cachedAttribution ?: client?.peekAttribution()?.also { cachedAttribution = it }

    /** 媒体渠道；归因未就绪时为 null。 */
    val network: String? get() = attribution?.network

    /** 广告活动；归因未就绪时为 null。 */
    val campaign: String? get() = attribution?.campaign

    /** 广告组；归因未就绪时为 null。 */
    val adGroup: String? get() = attribution?.adGroup

    /** 创意；归因未就绪时为 null。 */
    val creative: String? get() = attribution?.creative

    /** 点击标签；归因未就绪时为 null。 */
    val clickLabel: String? get() = attribution?.clickLabel

    /**
     * 是否自然量。
     * - `true` / `false`：已有归因结果
     * - `null`：归因尚未就绪
     */
    val isOrganic: Boolean? get() = attribution?.isOrganic

    /**
     * Runtime switch for MmpKit logs. Also set automatically from [MmpConfig.enableLog] in [init].
     */
    fun setLogEnabled(enabled: Boolean) {
        MmpLog.enabled = enabled
        MmpLog.i("setLogEnabled($enabled)")
    }

    fun isLogEnabled(): Boolean = MmpLog.enabled

    fun init(context: Context, config: MmpConfig, client: MmpClient) {
        synchronized(lock) {
            MmpLog.enabled = config.enableLog
            MmpLog.i(
                "init start: client=${client.javaClass.simpleName}, " +
                    "token=${MmpLog.maskToken(config.appToken)}, " +
                    "isDebug=${config.isDebug}, appId=${config.appId}, enableLog=${config.enableLog}",
            )
            cachedAttribution = null
            this.client = client
            client.initialize(context.applicationContext, config)
            // Keep facade cache in sync even if the app never sets a listener.
            client.setAttributionListener(internalAttributionListener)
            MmpLog.i("init done")
        }
    }

    fun setAttributionListener(listener: AttributionListener?) {
        MmpLog.d("setAttributionListener(listener=${listener != null})")
        userAttributionListener = listener
        // Re-deliver cache to the new listener immediately when available.
        attribution?.let { listener?.onAttribution(it) }
    }

    fun setDeepLinkListener(listener: DeepLinkListener?) {
        MmpLog.d("setDeepLinkListener(listener=${listener != null})")
        withClient("setDeepLinkListener") { it.setDeepLinkListener(listener) }
    }

    fun trackEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        trackEvent(MmpEvent(name = name, params = params))
    }

    fun trackEvent(event: MmpEvent) {
        MmpLog.d("trackEvent(name=${event.name}, params=${event.params})")
        withClient("trackEvent") { it.trackEvent(event) }
    }

    fun trackRevenue(revenue: MmpRevenue) {
        MmpLog.d(
            "trackRevenue(name=${revenue.eventName}, revenue=${revenue.revenue}, " +
                "currency=${revenue.currency}, params=${revenue.params})",
        )
        withClient("trackRevenue") { it.trackRevenue(revenue) }
    }

    /**
     * Async fetch (may hit the vendor SDK). Also refreshes the local cache.
     * For common checks prefer [attribution] / [isOrganic] / [network].
     */
    fun getAttribution(callback: (AttributionData?) -> Unit) {
        MmpLog.d("getAttribution()")
        val current = client
        if (current == null) {
            MmpLog.w("getAttribution called before init; returning null")
            callback(null)
            return
        }
        current.getAttribution { data ->
            if (data != null) {
                cachedAttribution = data
            }
            MmpLog.d("getAttribution result=$data")
            callback(data)
        }
    }

    private val internalAttributionListener = AttributionListener { data ->
        cachedAttribution = data
        MmpLog.d("attribution cached: network=${data.network}, campaign=${data.campaign}, organic=${data.isOrganic}")
        userAttributionListener?.onAttribution(data)
    }

    private inline fun withClient(action: String, block: (MmpClient) -> Unit) {
        val current = client
        if (current == null) {
            MmpLog.w("$action called before init; no-op")
            return
        }
        block(current)
    }
}
