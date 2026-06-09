/*
 * Copyright (C) 2026 The uwuAOSP Project
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

package com.android.launcher3.smartspacer

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.util.Themes
import com.android.launcher3.views.OptionsPopupView
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.SMARTSPACER_PACKAGE_NAME
import com.kieronquinn.app.smartspacer.sdk.client.R as SmartspacerR
import com.kieronquinn.app.smartspacer.sdk.client.views.BcSmartspaceView
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.Popup
import com.kieronquinn.app.smartspacer.sdk.client.views.popup.PopupFactory
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceConfig
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface

class LauncherSmartspacerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BcSmartspaceView(context, attrs) {

    override val config = SmartspaceConfig(
        smartspaceTargetCount = 5,
        uiSurface = UiSurface.HOMESCREEN,
        packageName = context.packageName,
    )

    init {
        setTintColour(Themes.getAttrColor(context, R.attr.workspaceTextColor))
        setApplyShadowIfRequired(!Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText))
        popupFactory = LauncherPopupFactory()
    }

    private inner class LauncherPopupFactory : PopupFactory {
        override fun createPopup(
            context: Context,
            anchorView: View,
            target: SmartspaceTarget,
            backgroundColor: Int,
            textColour: Int,
            launchIntent: (Intent?) -> Unit,
            dismissAction: ((SmartspaceTarget) -> Unit)?,
            aboutIntent: Intent?,
            feedbackIntent: Intent?,
            settingsIntent: Intent?,
        ): Popup {
            val launcher = Launcher.getLauncher(context)
            val pos = Rect()
            launcher.dragLayer.getDescendantRectRelativeToSelf(anchorView, pos)
            val options = listOfNotNull(
                createAboutOption(context, launchIntent, aboutIntent),
                createSettingsOption(context, launchIntent, settingsIntent),
                createFeedbackOption(context, launchIntent, feedbackIntent),
                createDismissOption(context, target, dismissAction),
            ).ifEmpty {
                listOf(createSettingsFallbackOption(context))
            }
            val popup = OptionsPopupView.show<Launcher>(launcher, RectF(pos), options, true)
            return object : Popup {
                override fun dismiss() {
                    popup.close(true)
                }
            }
        }
    }

    private fun createDismissOption(
        context: Context,
        target: SmartspaceTarget,
        dismissAction: ((SmartspaceTarget) -> Unit)?,
    ): OptionsPopupView.OptionItem? {
        if (dismissAction == null) return null
        return OptionsPopupView.OptionItem(
            context,
            SmartspacerR.string.smartspace_long_press_popup_dismiss,
            SmartspacerR.drawable.ic_smartspace_long_press_dismiss,
            StatsLogManager.LauncherEvent.IGNORE,
        ) {
            dismissAction.invoke(target)
            true
        }
    }

    private fun createAboutOption(
        context: Context,
        launchIntent: (Intent?) -> Unit,
        aboutIntent: Intent?,
    ): OptionsPopupView.OptionItem? {
        if (aboutIntent == null) return null
        return OptionsPopupView.OptionItem(
            context,
            SmartspacerR.string.smartspace_long_press_popup_about,
            SmartspacerR.drawable.ic_smartspace_long_press_about,
            StatsLogManager.LauncherEvent.IGNORE,
        ) {
            launchIntent(aboutIntent)
            true
        }
    }

    private fun createFeedbackOption(
        context: Context,
        launchIntent: (Intent?) -> Unit,
        feedbackIntent: Intent?,
    ): OptionsPopupView.OptionItem? {
        if (feedbackIntent == null) return null
        return OptionsPopupView.OptionItem(
            context,
            SmartspacerR.string.smartspace_long_press_popup_feedback,
            SmartspacerR.drawable.ic_smartspace_long_press_feedback,
            StatsLogManager.LauncherEvent.IGNORE,
        ) {
            launchIntent(feedbackIntent)
            true
        }
    }

    private fun createSettingsOption(
        context: Context,
        launchIntent: (Intent?) -> Unit,
        settingsIntent: Intent?,
    ): OptionsPopupView.OptionItem? {
        if (settingsIntent == null) return null
        return OptionsPopupView.OptionItem(
            context,
            R.string.settings_button_text,
            R.drawable.ic_setting,
            StatsLogManager.LauncherEvent.IGNORE,
        ) {
            launchIntent(settingsIntent)
            true
        }
    }

    private fun createSettingsFallbackOption(context: Context): OptionsPopupView.OptionItem {
        return OptionsPopupView.OptionItem(
            context,
            R.string.settings_button_text,
            R.drawable.ic_setting,
            StatsLogManager.LauncherEvent.IGNORE,
        ) {
            val intent = context.packageManager.getLaunchIntentForPackage(SMARTSPACER_PACKAGE_NAME)
            if (intent != null) {
                Launcher.getLauncher(context).startActivitySafely(this, intent, null)
                true
            } else {
                false
            }
        }
    }
}
