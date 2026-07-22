package com.kit.mmp.sample

import android.app.Application
import com.kit.mmp.Mmp
import com.kit.mmp.MmpClient
import com.kit.mmp.adjust.AdjustClient
import com.kit.mmp.appsflyer.AppsFlyerClient
import com.kit.mmp.config.MmpConfig
import com.kit.mmp.sample.demo.DemoStore

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val platform = BuildConfig.MMP_PLATFORM.trim().lowercase()
        val client: MmpClient = when (platform) {
            "adjust" -> AdjustClient()
            else -> AppsFlyerClient()
        }
        val platformLabel = when (client) {
            is AdjustClient -> "Adjust"
            else -> "AppsFlyer"
        }

        DemoStore.setPlatform(platformLabel)
        Mmp.init(
            context = this,
            config = MmpConfig(
                appToken = BuildConfig.MMP_APP_TOKEN,
                isDebug = BuildConfig.MMP_IS_DEBUG,
                appId = BuildConfig.MMP_APP_ID.ifBlank { null },
                enableLog = BuildConfig.MMP_ENABLE_LOG,
            ),
            client = client,
        )
        DemoStore.setLogEnabled(BuildConfig.MMP_ENABLE_LOG)
        DemoStore.append(
            "Mmp initialized from local.properties " +
                "(platform=$platformLabel, token=${mask(BuildConfig.MMP_APP_TOKEN)})",
        )

        Mmp.setAttributionListener { data ->
            DemoStore.onAttribution(data)
        }
        Mmp.setDeepLinkListener { result ->
            DemoStore.onDeepLink(result)
        }
    }

    private fun mask(token: String): String {
        if (token.length <= 8) return "****"
        return "${token.take(4)}****${token.takeLast(4)}"
    }
}
