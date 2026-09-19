package com.shenghui.localvibe.feature.book

internal object BookPlaybackSessionGuard {
    fun isActive(callbackSessionId: Long, activeSessionId: Long, screenDisposed: Boolean): Boolean {
        return callbackSessionId == activeSessionId && !screenDisposed
    }
}
