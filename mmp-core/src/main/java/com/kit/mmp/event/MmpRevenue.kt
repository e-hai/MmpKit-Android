package com.kit.mmp.event

/**
 * @param currency ISO 4217 currency code, e.g. "USD".
 */
data class MmpRevenue(
    val revenue: Double,
    val currency: String,
    val eventName: String = "purchase",
    val params: Map<String, Any?> = emptyMap(),
)
