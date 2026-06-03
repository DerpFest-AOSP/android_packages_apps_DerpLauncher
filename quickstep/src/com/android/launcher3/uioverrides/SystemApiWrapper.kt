/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3.uioverrides

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.role.RoleManager
import android.app.smartspace.SmartspaceConfig
import android.app.smartspace.SmartspaceManager
import android.app.smartspace.SmartspaceSession
import android.app.smartspace.SmartspaceTarget
import android.content.ComponentName
import android.content.Context
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.Rect
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.UserHandle
import android.text.TextUtils
import android.util.Log
import android.view.DragAndDropPermissions
import android.view.DragEvent
import android.view.SurfaceControlViewHost
import android.widget.Toast
import android.window.RemoteTransition
import android.window.ScreenCapture.ScreenCaptureParams
import android.window.ScreenCaptureInternal
import android.app.smartspace.uitemplatedata.Icon as SmartspaceTemplateIcon
import com.android.launcher3.BaseActivity
import com.android.launcher3.Flags.enablePrivateSpace
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.notification.NotificationListener
import com.android.launcher3.proxy.ProxyActivityStarter
import com.android.launcher3.uioverrides.touchcontrollers.StatusBarTouchController
import com.android.launcher3.util.ApiWrapper
import com.android.launcher3.util.Executors
import com.android.launcher3.util.StartActivityParams
import com.android.quickstep.util.FadeOutRemoteTransition
import java.util.function.Supplier
import javax.inject.Inject

/** A wrapper for the hidden API calls */
@LauncherAppSingleton
open class SystemApiWrapper @Inject constructor(@ApplicationContext context: Context?) :
    ApiWrapper(context) {

    companion object {
        private const val TAG = "SystemApiWrapper"
        private const val HOME_SMARTSPACE_SURFACE = "home"
    }

    override fun getPersons(si: ShortcutInfo) = si.persons ?: Utilities.EMPTY_PERSON_ARRAY

    override fun getActivityOverrides(): Map<String, LauncherActivityInfo> =
        mContext.getSystemService(LauncherApps::class.java)!!.activityOverrides

    override fun createFadeOutAnimOptions(): ActivityOptions =
        ActivityOptions.makeBasic().apply {
            remoteTransition = RemoteTransition(FadeOutRemoteTransition(), "FadeOut")
        }

    override fun getAppMarketActivityIntent(packageName: String, user: UserHandle): Intent =
        if (enablePrivateSpace())
            ProxyActivityStarter.getLaunchIntent(
                mContext,
                StartActivityParams(null as PendingIntent?, 0).apply {
                    intentSender =
                        mContext
                            .getSystemService(LauncherApps::class.java)!!
                            .getAppMarketActivityIntent(packageName, user)
                    options =
                        ActivityOptions.makeBasic()
                            .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                            .toBundle()
                    requireActivityResult = false
                },
            )
        else super.getAppMarketActivityIntent(packageName, user)

    /** Returns an intent which can be used to open Private Space Settings. */
    override fun getPrivateSpaceSettingsIntent(): Intent? =
        if (enablePrivateSpace())
            ProxyActivityStarter.getLaunchIntent(
                mContext,
                StartActivityParams(null as PendingIntent?, 0).apply {
                    intentSender =
                        mContext
                            .getSystemService(LauncherApps::class.java)
                            ?.privateSpaceSettingsIntent ?: return null
                    options =
                        ActivityOptions.makeBasic()
                            .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                            .toBundle()
                    requireActivityResult = false
                },
            )
        else null

    override fun isNonResizeableActivity(lai: LauncherActivityInfo) =
        lai.activityInfo.resizeMode == ActivityInfo.RESIZE_MODE_UNRESIZEABLE

    override fun supportsMultiInstance(lai: LauncherActivityInfo): Boolean {
        return try {
            super.supportsMultiInstance(lai) || lai.supportsMultiInstance()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Starts an Activity which can be used to set this Launcher as the HOME app, via a consent
     * screen. In case the consent screen cannot be shown, or the user does not set current Launcher
     * as HOME app, a toast asking the user to do the latter is shown.
     */
    override fun assignDefaultHomeRole(context: Context) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (
            (roleManager!!.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME))
        ) {
            val roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            val pendingIntent =
                PendingIntent(
                    object : IIntentSender.Stub() {
                        override fun send(
                            code: Int,
                            intent: Intent,
                            resolvedType: String?,
                            allowlistToken: IBinder?,
                            finishedReceiver: IIntentReceiver?,
                            requiredPermission: String?,
                            options: Bundle?,
                        ) {
                            if (code != -1) {
                                Executors.MAIN_EXECUTOR.execute {
                                    Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.set_default_home_app,
                                                context.getString(R.string.derived_app_name),
                                            ),
                                            Toast.LENGTH_LONG,
                                        )
                                        .show()
                                }
                            }
                        }
                    }
                )
            val params = StartActivityParams(pendingIntent, 0)
            params.intent = roleRequestIntent
            context.startActivity(ProxyActivityStarter.getLaunchIntent(context, params))
        }
    }

    override fun createStatusBarTouchController(
        launcher: BaseActivity,
        isEnabledCheck: Supplier<Boolean>,
    ): StatusBarTouchController? {
        return StatusBarTouchController(launcher, isEnabledCheck)
    }

    override fun captureSnapshot(host: SurfaceControlViewHost, width: Int, height: Int): Bitmap =
        ScreenCaptureInternal.captureLayers(
                ScreenCaptureInternal.LayerCaptureArgs.Builder(host.surfacePackage!!.surfaceControl)
                    .setSourceCrop(Rect(0, 0, width, height))
                    .setProtectedContentPolicy(ScreenCaptureParams.PROTECTED_CONTENT_POLICY_CAPTURE)
                    .setPreserveDisplayColors(true)
                    .build()
            )
            .asBitmap()
            .copy(Bitmap.Config.ARGB_8888, true)

    override fun requestDragAndDropPermissions(event: DragEvent): DragAndDropPermissions? =
        DragAndDropPermissions.obtain(event)

    override fun createWeatherDataProvider(): WeatherDataProvider = SmartspaceWeatherDataProvider()

    override fun createMediaDataProvider(): MediaDataProvider = ActiveMediaDataProvider()

    private inner class SmartspaceWeatherDataProvider : WeatherDataProvider {
        private var callback: WeatherInfoListener? = null
        private var smartspaceSession: SmartspaceSession? = null
        private val listener =
            SmartspaceSession.OnTargetsAvailableListener { targets ->
                callback?.onWeatherInfoUpdated(extractWeatherInfo(targets))
            }

        override fun setCallback(callback: WeatherInfoListener?) {
            this.callback = callback
        }

        override fun start() {
            if (smartspaceSession != null) {
                return
            }
            val smartspaceManager = mContext.getSystemService(SmartspaceManager::class.java)
            if (smartspaceManager == null) {
                callback?.onWeatherInfoUpdated(null)
                return
            }
            try {
                smartspaceSession =
                    smartspaceManager.createSmartspaceSession(
                        SmartspaceConfig.Builder(mContext, HOME_SMARTSPACE_SURFACE)
                            .setSmartspaceTargetCount(1)
                            .build()
                    )
                smartspaceSession?.addOnTargetsAvailableListener(Executors.MAIN_EXECUTOR, listener)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start weather smartspace session", e)
                callback?.onWeatherInfoUpdated(null)
            }
        }

        override fun stop() {
            val session = smartspaceSession ?: return
            smartspaceSession = null
            try {
                session.removeOnTargetsAvailableListener(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister weather listener", e)
            }
            try {
                session.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to close weather smartspace session", e)
            }
        }

        private fun extractWeatherInfo(targets: List<SmartspaceTarget>): WeatherInfo? {
            val now = System.currentTimeMillis()
            val target =
                targets.firstOrNull {
                    it.featureType == SmartspaceTarget.FEATURE_WEATHER &&
                        now >= it.creationTimeMillis &&
                        now <= it.expiryTimeMillis
                } ?: return null

            return weatherInfoFromHeaderAction(target)
                ?: weatherInfoFromTemplate(target)
        }

        private fun weatherInfoFromHeaderAction(target: SmartspaceTarget): WeatherInfo? {
            val action = target.headerAction ?: return null
            val text = action.title
            if (TextUtils.isEmpty(text)) {
                return null
            }
            return WeatherInfo(
                text,
                loadDrawable(action.icon),
                false,
            )
        }

        private fun weatherInfoFromTemplate(target: SmartspaceTarget): WeatherInfo? {
            val subtitleItem = target.templateData?.subtitleItem ?: return null
            val text = subtitleItem.text?.text ?: return null
            if (TextUtils.isEmpty(text)) {
                return null
            }
            val icon = subtitleItem.icon
            return WeatherInfo(
                text,
                loadDrawable(icon),
                icon?.shouldTint() ?: true,
            )
        }

        private fun loadDrawable(icon: android.graphics.drawable.Icon?): Drawable? {
            return icon?.loadDrawable(mContext)
        }

        private fun loadDrawable(icon: SmartspaceTemplateIcon?): Drawable? {
            return icon?.icon?.loadDrawable(mContext)
        }
    }

    private inner class ActiveMediaDataProvider : MediaDataProvider {
        private var callback: MediaInfoListener? = null
        private val mediaSessionManager = mContext.getSystemService(MediaSessionManager::class.java)
        private val listenerComponent = ComponentName(mContext, NotificationListener::class.java)
        private var mediaController: MediaController? = null
        private val sessionsListener =
            MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                updateActiveController(controllers.orEmpty())
            }
        private val mediaCallback =
            object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    dispatchCurrentMedia()
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    dispatchCurrentMedia()
                }

                override fun onSessionDestroyed() {
                    clearController()
                    callback?.onMediaInfoUpdated(null)
                }
            }

        override fun setCallback(callback: MediaInfoListener?) {
            this.callback = callback
        }

        override fun start() {
            if (mediaSessionManager == null) {
                callback?.onMediaInfoUpdated(null)
                return
            }
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(sessionsListener, null)
                updateActiveController(mediaSessionManager.getActiveSessions(null))
            } catch (e: SecurityException) {
                try {
                    mediaSessionManager.addOnActiveSessionsChangedListener(
                        sessionsListener,
                        listenerComponent,
                    )
                    updateActiveController(mediaSessionManager.getActiveSessions(listenerComponent))
                } catch (inner: SecurityException) {
                    Log.w(TAG, "Failed to register active media sessions listener", inner)
                    callback?.onMediaInfoUpdated(null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start media session monitoring", e)
                callback?.onMediaInfoUpdated(null)
            }
        }

        override fun stop() {
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister active media sessions listener", e)
            }
            clearController()
        }

        private fun updateActiveController(controllers: List<MediaController>) {
            val best = pickBestController(controllers)
            if (best?.sessionToken == mediaController?.sessionToken) {
                dispatchCurrentMedia()
                return
            }
            clearController()
            mediaController = best
            best?.registerCallback(mediaCallback, android.os.Handler(mContext.mainLooper))
            dispatchCurrentMedia()
        }

        private fun pickBestController(controllers: List<MediaController>): MediaController? {
            var fallback: MediaController? = null
            controllers.forEach { controller ->
                val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
                if (state == PlaybackState.STATE_PLAYING) {
                    return controller
                }
                if (fallback == null && controller.metadata != null) {
                    fallback = controller
                }
            }
            return fallback ?: controllers.firstOrNull()
        }

        private fun dispatchCurrentMedia() {
            callback?.onMediaInfoUpdated(extractMediaInfo(mediaController))
        }

        private fun extractMediaInfo(controller: MediaController?): MediaInfo? {
            val currentController = controller ?: return null
            val playbackState = currentController.playbackState ?: return null
            if (!playbackState.isActiveCompat()) {
                return null
            }

            val metadata = currentController.metadata
            val description = metadata?.description
            val title =
                firstNonEmpty(
                    metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                    description?.title,
                    metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                ) ?: return null
            val subtitle =
                firstNonEmpty(
                    metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    description?.subtitle,
                    metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
                    metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                )

            return MediaInfo(
                title,
                subtitle,
                loadMediaDrawable(metadata, description),
                false,
            )
        }

        private fun firstNonEmpty(vararg texts: CharSequence?): CharSequence? {
            return texts.firstOrNull { !TextUtils.isEmpty(it) }
        }

        private fun loadMediaDrawable(
            metadata: MediaMetadata?,
            description: MediaDescription?
        ): Drawable? {
            val bitmap =
                metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: description?.iconBitmap
            if (bitmap != null) {
                return BitmapDrawable(mContext.resources, bitmap)
            }

            val uri =
                firstNonEmpty(
                    metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
                    metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                    metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI),
                    description?.iconUri?.toString(),
                )
            return uri?.let(::loadMediaDrawableFromUri)
        }

        private fun loadMediaDrawableFromUri(uriString: CharSequence): Drawable? {
            return try {
                mContext.contentResolver.openInputStream(Uri.parse(uriString.toString()))?.use {
                    Drawable.createFromStream(it, uriString.toString())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load media artwork from uri", e)
                null
            }
        }

        private fun clearController() {
            mediaController?.unregisterCallback(mediaCallback)
            mediaController = null
        }
    }

    private fun PlaybackState.isActiveCompat(): Boolean {
        return when (state) {
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> true
            else -> false
        }
    }
}
