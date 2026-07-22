package com.kit.mmp.event

data class MmpEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
)
