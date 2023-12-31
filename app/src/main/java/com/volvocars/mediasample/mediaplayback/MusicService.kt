/*
 * Copyright 2022 Volvo Car Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.volvocars.mediasample.mediaplayback

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.volvocars.mediasample.common.logging.logd
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MusicService : MediaBrowserServiceCompat() {
    private val mediaLibrary: MediaLibrary by inject()
    private val mediaPlayer: Player by inject { parametersOf(this) }
    private val mediaSession: MediaSessionCompat by inject { parametersOf(this) }
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
                packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                    PendingIntent.getActivity(this, 0, sessionIntent, 0)
                }

        mediaSession.setSessionActivity(sessionActivityPendingIntent)
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(mediaPlayer)
        mediaSessionConnector.setPlaybackPreparer(PlaybackPreparer(mediaPlayer, mediaLibrary))
        sessionToken = mediaSession.sessionToken
        logd("media session created with token: $sessionToken")
    }

    override fun onDestroy() {
        logd("service destroyed!")
        mediaSession.run {
            isActive = false
            release()
        }
        mediaPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // TODO verify calling package
        val supportedRootChildFlags = rootHints?.getInt(
            MediaConstants.BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_SUPPORTED_FLAGS,
            /* defaultValue= */ MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
        logd(supportedRootChildFlags.toString())
        val extras = Bundle()
        extras.putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        extras.putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )

        return BrowserRoot(BROWSABLE_ROOT_ID, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mediaLibrary.getPlaylistById(parentId)?.map { it.toMediaItem() }
        result.sendResult(mediaItems)
    }
}