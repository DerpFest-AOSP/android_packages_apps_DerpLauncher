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

import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.graphics.ThemeManager;

/**
 * Settings activity for app drawer preferences.
 */
public class SettingsAppDrawer extends SettingsCategoryActivity {

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.app_drawer_settings_fragment_name);
    }

    public static class AppDrawerSettingsFragment extends CategorySettingsFragment {

        private Preference mThemeAllAppsIconsPref;

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_app_drawer_preferences;
        }

        @Override
        protected boolean initPreference(Preference preference) {
            if (LauncherPrefs.ALLAPPS_THEMED_ICONS.getSharedPrefKey().equals(preference.getKey())) {
                mThemeAllAppsIconsPref = preference;
                updateThemeAllAppsIconsPref();
            }
            return super.initPreference(preference);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mThemeAllAppsIconsPref != null) {
                updateThemeAllAppsIconsPref();
            }
        }

        private void updateThemeAllAppsIconsPref() {
            boolean enabled = ThemeManager.INSTANCE.get(getContext()).isMonoThemeEnabled();
            mThemeAllAppsIconsPref.setEnabled(enabled);
            mThemeAllAppsIconsPref.setSummary(getContext().getString(enabled
                    ? R.string.pref_themed_icons_summary
                    : R.string.themed_icons_disabled_summary));
        }
    }
}
