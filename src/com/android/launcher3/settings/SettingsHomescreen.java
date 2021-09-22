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

import android.content.SharedPreferences;

import androidx.preference.Preference;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.derpfest.DerpFestUtils;

/**
 * Settings activity for home screen preferences.
 */
public class SettingsHomescreen extends SettingsCategoryActivity {

    private static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.home_screen_settings_fragment_name);
    }

    public static class HomescreenSettingsFragment extends CategorySettingsFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_home_screen_preferences;
        }

        @Override
        protected boolean initPreference(Preference preference) {
            if (LauncherPrefs.ENABLE_MINUS_ONE.getSharedPrefKey().equals(preference.getKey())) {
                return DerpFestUtils.isPackageEnabled(getContext(), SEARCH_PACKAGE);
            }
            return super.initPreference(preference);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (LauncherPrefs.SHOW_HOTSEAT_BG.getSharedPrefKey().equals(key)) {
                LauncherAppState.INSTANCE.get(getContext()).setNeedsRestart();
            }
        }
    }
}
