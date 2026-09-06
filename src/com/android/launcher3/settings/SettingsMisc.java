/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.settings;

import static com.android.launcher3.InvariantDeviceProfile.TYPE_MULTI_DISPLAY;
import static com.android.launcher3.InvariantDeviceProfile.TYPE_TABLET;
import static com.android.launcher3.settings.SettingsActivity.FIXED_LANDSCAPE_MODE;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import androidx.preference.Preference;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.derpfest.DerpFestUtils;
import com.android.launcher3.display.DisplayController;
import com.android.launcher3.display.LauncherDisplayInfo;
import com.android.systemui.shared.system.BlurUtils;

/**
 * Settings activity for miscellaneous launcher preferences.
 */
public class SettingsMisc extends SettingsCategoryActivity {

    private static final String KEY_SUGGESTIONS = "pref_suggestions";
    private static final String SUGGESTIONS_PACKAGE = "com.google.android.as";

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.misc_settings_fragment_name);
    }

    public static class MiscSettingsFragment extends CategorySettingsFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_misc_preferences;
        }

        @Override
        protected boolean initPreference(Preference preference) {
            LauncherDisplayInfo info = DisplayController.INSTANCE.get(getContext()).getInfo();
            if (FIXED_LANDSCAPE_MODE.equals(preference.getKey())) {
                if ((InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                                == TYPE_MULTI_DISPLAY)
                        || (InvariantDeviceProfile.INSTANCE.get(getContext()).deviceType
                                == TYPE_TABLET)) {
                    return false;
                }
                preference.setOnPreferenceChangeListener(
                        (pref, newValue) -> {
                            getActivity().setRequestedOrientation(
                                    (boolean) newValue
                                            ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            : ActivityInfo.SCREEN_ORIENTATION_USER
                            );
                            return true;
                        }
                );
                return !info.isLargeScreen(info.realBounds);
            } else if (KEY_SUGGESTIONS.equals(preference.getKey())) {
                return DerpFestUtils.isPackageEnabled(getContext(), SUGGESTIONS_PACKAGE);
            } else if (LauncherPrefs.BLUR_DEPTH.getSharedPrefKey().equals(preference.getKey())) {
                return BlurUtils.supportsBlursOnWindows();
            }
            return true;
        }

        @Override
        public void onStart() {
            super.onStart();
            LauncherPrefs.getPrefs(getContext()).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            LauncherPrefs.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (LauncherPrefs.BLUR_DEPTH.getSharedPrefKey().equals(key)) {
                LauncherAppState.INSTANCE.get(getContext()).setNeedsRestart();
            }
        }
    }
}
