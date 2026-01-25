/*
 * Copyright (C) 2018 The Dirty Unicorns Project
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

package com.android.launcher3.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.util.derp.derpUtils;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherState;

/**
 * Shared home-screen gesture actions used by double-tap and swipe-down.
 */
public final class GestureActions {

    private GestureActions() {}

    public static int getDoubleTapMode(Context context) {
        SharedPreferences prefs = LauncherPrefs.getPrefs(context);
        if (!prefs.contains(LauncherPrefs.HOMESCREEN_DT_GESTURES.getSharedPrefKey())
                && prefs.contains("pref_sleep_gesture")) {
            return prefs.getBoolean("pref_sleep_gesture", true) ? 1 : 0;
        }
        return parseMode(LauncherPrefs.HOMESCREEN_DT_GESTURES.get(context), 1);
    }

    public static int getSwipeDownMode(Context context) {
        return parseMode(LauncherPrefs.HOMESCREEN_SWIPE_DOWN_GESTURES.get(context), 0);
    }

    public static int getSwipeDownSide(Context context) {
        return parseMode(LauncherPrefs.SWIPE_DOWN_SIDE.get(context), 0);
    }

    public static void execute(
            Context context, @Nullable Launcher launcher, int gestureType, boolean isSwipeDown) {
        if (gestureType == 0) {
            return;
        }
        boolean haptics = isSwipeDown
                ? LauncherPrefs.HAPTICS_ON_SWIPE_DOWN_GESTURES.get(context)
                : LauncherPrefs.HAPTICS_ON_DT_GESTURES.get(context);
        if (haptics) {
            VibratorWrapper.INSTANCE.get(context).vibrate(VibratorWrapper.EFFECT_CLICK);
        }
        switch (gestureType) {
            case 1:
                derpUtils.switchScreenOff(context);
                break;
            case 2:
                derpUtils.toggleCameraFlash();
                break;
            case 3:
                derpUtils.toggleVolumePanel(context);
                break;
            case 4:
                derpUtils.clearAllNotifications();
                break;
            case 5:
                derpUtils.takeScreenshot(true);
                break;
            case 6:
                derpUtils.toggleNotifications();
                break;
            case 7:
                derpUtils.toggleQsPanel();
                break;
            case 8:
                derpUtils.showPowerMenu();
                break;
            case 9:
                clearAllApps(launcher);
                break;
            default:
                break;
        }
    }

    public static boolean isSwipeDownAllowedOnSide(Context context, float x) {
        int sideMode = getSwipeDownSide(context);
        if (sideMode == 3) {
            return false;
        }
        float screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        boolean isRtl = context.getResources().getConfiguration().getLayoutDirection()
                == android.view.View.LAYOUT_DIRECTION_RTL;
        return switch (sideMode) {
            case 1 -> isRtl ? (x > screenWidth / 2) : (x < screenWidth / 2);
            case 2 -> isRtl ? (x < screenWidth / 2) : (x > screenWidth / 2);
            default -> true;
        };
    }

    private static void clearAllApps(@Nullable Launcher launcher) {
        if (launcher == null) {
            return;
        }
        launcher.getStateManager().goToState(LauncherState.OVERVIEW, true);
        launcher.getDragLayer().post(() -> {
            View overview = launcher.getOverviewPanel();
            if (overview == null) {
                return;
            }
            try {
                overview.getClass().getMethod("dismissAllTasks").invoke(overview);
            } catch (ReflectiveOperationException ignored) {
                // Overview panel is only RecentsView in the Quickstep build.
            }
        });
    }

    private static int parseMode(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
