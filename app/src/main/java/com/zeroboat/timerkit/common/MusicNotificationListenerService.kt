package com.zeroboat.timerkit.common

import android.service.notification.NotificationListenerService

/**
 * MediaSessionManager.getActiveSessions() 호출을 위해 필요한 서비스.
 * 실제 알림 처리는 하지 않으며, NotificationListenerService 바인딩 권한 획득 목적으로만 사용.
 */
class MusicNotificationListenerService : NotificationListenerService()
