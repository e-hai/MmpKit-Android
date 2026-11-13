package com.kit.mmp

import android.content.Context
import android.content.Intent
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.attribution.AttributionListener
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.deeplink.DeepLinkListener
import com.kit.mmp.event.MmpEvent
import com.kit.mmp.event.MmpRevenue

/**
 * Platform adapter contract. AppsFlyer / Adjust / etc. implement this.
 */
interface MmpClient {
    fun initialize(context: Context, config: MmpConfig)
    fun setAttributionListener(listener: AttributionListener?)
    fun setDeepLinkListener(listener: DeepLinkListener?)
    fun trackEvent(event: MmpEvent)
    fun trackRevenue(revenue: MmpRevenue)
    fun getAttribution(callback: (AttributionData?) -> Unit)

    /** Returns cached attribution immediately, or null if not yet available. */
    fun peekAttribution(): AttributionData?

    /**
     * Forwards a direct deep link [Intent] (from Activity.onCreate / onNewIntent) to the vendor SDK.
     */
    fun handleDeepLink(context: Context, intent: Intent?) {}
}
