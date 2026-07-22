package com.kit.mmp.config

/**
 * Shared MMP configuration.
 *
 * [appToken] maps to AppsFlyer devKey or Adjust appToken.
 * Platform-specific options belong on each adapter, not here.
 *
 * @param enableLog enables MmpKit verbose logs ([com.kit.mmp.log.MmpLog]).
 *   Platform SDK debug logs still follow [isDebug].
 */
data class MmpConfig(
    val appToken: String,
    val isDebug: Boolean = false,
    val appId: String? = null,
    val enableLog: Boolean = false,
)
