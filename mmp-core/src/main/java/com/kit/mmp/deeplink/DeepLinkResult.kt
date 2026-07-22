package com.kit.mmp.deeplink

data class DeepLinkResult(
    val deepLinkValue: String?,
    val clickEvent: Map<String, Any?> = emptyMap(),
    val isDeferred: Boolean = false,
)
