package com.kit.mmp.log

import android.util.Log

/**
 * MmpKit internal logger. Disabled by default; enable via [MmpConfig.enableLog]
 * or [com.kit.mmp.Mmp.setLogEnabled].
 */
object MmpLog {
    const val TAG = "MmpKit"

    @Volatile
    var enabled: Boolean = false

    fun d(message: String, tag: String = TAG) {
        if (enabled) Log.d(tag, message)
    }

    fun i(message: String, tag: String = TAG) {
        if (enabled) Log.i(tag, message)
    }

    fun w(message: String, tag: String = TAG) {
        if (enabled) Log.w(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (!enabled) return
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /** Masks secrets for safe logging, e.g. `abcd****wxyz`. */
    fun maskToken(token: String?): String {
        if (token.isNullOrEmpty()) return "<empty>"
        if (token.length <= 8) return "****"
        return "${token.take(4)}****${token.takeLast(4)}"
    }
}
