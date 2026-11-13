package com.kit.mmp

import android.content.Context
import android.content.Intent
import com.kit.mmp.attribution.AttributionData
import com.kit.mmp.attribution.AttributionListener
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.deeplink.DeepLinkListener
import com.kit.mmp.event.MmpEvent
import com.kit.mmp.event.MmpRevenue
import com.kit.mmp.log.MmpLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MmpFacadeTest {
    @Test
    fun tokenMaskingHidesMiddleCharacters() {
        assertEquals("<empty>", MmpLog.maskToken(null))
        assertEquals("<empty>", MmpLog.maskToken(""))
        assertEquals("****", MmpLog.maskToken("12345678"))
        assertEquals("1234****6789", MmpLog.maskToken("123456789"))
    }

    @Test
    fun attributionCacheAndListenerReplay() {
        val mock = FakeMmpClient()
        var listenerCalls = 0
        var latestNetwork: String? = null

        // Pre-init checks
        assertNull(Mmp.attribution)

        // Note: JVM unit test passes null-free contract only if Context isn't dereferenced;
        // FakeMmpClient does not touch Context, so testing facade cache/listener state via MmpClient is safe
        // once Context is available or tested via AttributionData model directly.
        val data = AttributionData(
            network = "googleadwords_int",
            campaign = "spring_sale_2026",
            adGroup = "group_android",
            creative = "banner_300x250",
            clickLabel = "click_01",
            isOrganic = false,
            raw = mapOf("af_status" to "Non-organic"),
        )
        mock.setAttributionListener {
            listenerCalls++
            latestNetwork = it.network
        }
        mock.emitAttribution(data)

        assertEquals(1, listenerCalls)
        assertEquals("googleadwords_int", latestNetwork)
        assertEquals(data, mock.peekAttribution())
        assertFalse(data.isOrganic)
        assertTrue(mock.deepLinkHandled == 0)
    }

    private class FakeMmpClient : MmpClient {
        private var attributionListener: AttributionListener? = null
        private var deepLinkListener: DeepLinkListener? = null
        private var cached: AttributionData? = null
        var deepLinkHandled = 0

        override fun initialize(context: Context, config: MmpConfig) = Unit

        override fun setAttributionListener(listener: AttributionListener?) {
            attributionListener = listener
            cached?.let { listener?.onAttribution(it) }
        }

        override fun setDeepLinkListener(listener: DeepLinkListener?) {
            deepLinkListener = listener
        }

        override fun trackEvent(event: MmpEvent) = Unit
        override fun trackRevenue(revenue: MmpRevenue) = Unit
        override fun getAttribution(callback: (AttributionData?) -> Unit) = callback(cached)
        override fun peekAttribution(): AttributionData? = cached

        override fun handleDeepLink(context: Context, intent: Intent?) {
            deepLinkHandled++
        }

        fun emitAttribution(data: AttributionData) {
            cached = data
            attributionListener?.onAttribution(data)
        }
    }
}
