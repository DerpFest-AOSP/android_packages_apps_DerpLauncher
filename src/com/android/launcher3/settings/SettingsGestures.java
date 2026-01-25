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

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

/**
 * Settings activity for launcher gesture preferences.
 */
public class SettingsGestures extends SettingsCategoryActivity {

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.gesture_settings_fragment_name);
    }

    public static class GestureSettingsFragment extends CategorySettingsFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_gesture_preferences;
        }

        @Override
        public void onCreatePreferences(android.os.Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
            bindListPreference(LauncherPrefs.HOMESCREEN_DT_GESTURES.getSharedPrefKey());
            bindListPreference(LauncherPrefs.HOMESCREEN_SWIPE_DOWN_GESTURES.getSharedPrefKey());
            bindListPreference(LauncherPrefs.SWIPE_DOWN_SIDE.getSharedPrefKey());
            updateDependentPreferences();
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
            if (LauncherPrefs.HOMESCREEN_DT_GESTURES.getSharedPrefKey().equals(key)
                    || LauncherPrefs.HOMESCREEN_SWIPE_DOWN_GESTURES.getSharedPrefKey().equals(key)
                    || LauncherPrefs.SWIPE_DOWN_SIDE.getSharedPrefKey().equals(key)) {
                bindListPreference(key);
                updateDependentPreferences();
            }
        }

        private void bindListPreference(String key) {
            ListPreference preference = findPreference(key);
            if (preference == null) {
                return;
            }
            preference.setSummary(preference.getEntry());
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                ListPreference listPref = (ListPreference) pref;
                listPref.setValue((String) newValue);
                listPref.setSummary(listPref.getEntry());
                updateDependentPreferences();
                return true;
            });
        }

        private void updateDependentPreferences() {
            Preference dtHaptics = findPreference(
                    LauncherPrefs.HAPTICS_ON_DT_GESTURES.getSharedPrefKey());
            Preference swipeHaptics = findPreference(
                    LauncherPrefs.HAPTICS_ON_SWIPE_DOWN_GESTURES.getSharedPrefKey());
            Preference swipeSide = findPreference(
                    LauncherPrefs.SWIPE_DOWN_SIDE.getSharedPrefKey());

            boolean dtEnabled = !"0".equals(
                    LauncherPrefs.HOMESCREEN_DT_GESTURES.get(getContext()));
            boolean swipeEnabled = !"0".equals(
                    LauncherPrefs.HOMESCREEN_SWIPE_DOWN_GESTURES.get(getContext()));
            if (dtHaptics != null) {
                dtHaptics.setEnabled(dtEnabled);
            }
            if (swipeHaptics != null) {
                swipeHaptics.setEnabled(swipeEnabled);
            }
            if (swipeSide != null) {
                swipeSide.setEnabled(swipeEnabled);
            }
        }
    }
}
