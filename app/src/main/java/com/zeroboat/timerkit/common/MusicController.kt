package com.zeroboat.timerkit.common

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings

data class MusicState(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean
)

class MusicController(private val context: Context) {

    fun isPermissionGranted(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(context.packageName)
    }

    private fun getActiveController(): android.media.session.MediaController? {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager ?: return null
        return try {
            manager.getActiveSessions(
                ComponentName(context, MusicNotificationListenerService::class.java)
            ).firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }

    fun getCurrentState(): MusicState? {
        val ctrl = getActiveController() ?: return null
        val meta = ctrl.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
        if (title == null && artist == null) return null
        val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
        return MusicState(title = title, artist = artist, isPlaying = isPlaying)
    }

    fun togglePlayPause() {
        val ctrl = getActiveController() ?: return
        if (ctrl.playbackState?.state == PlaybackState.STATE_PLAYING) {
            ctrl.transportControls.pause()
        } else {
            ctrl.transportControls.play()
        }
    }

    fun previous() { getActiveController()?.transportControls?.skipToPrevious() }
    fun next() { getActiveController()?.transportControls?.skipToNext() }
}
