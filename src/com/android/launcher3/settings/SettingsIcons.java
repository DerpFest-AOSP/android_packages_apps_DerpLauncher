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

import com.android.launcher3.BuildConfig;
import com.android.launcher3.R;

import androidx.preference.Preference;

/**
 * Settings activity for icon preferences.
 */
public class SettingsIcons extends SettingsCategoryActivity {

    private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.icons_settings_fragment_name);
    }

    public static class IconsSettingsFragment extends CategorySettingsFragment {

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_icons_preferences;
        }

        @Override
        protected boolean initPreference(Preference preference) {
            if (NOTIFICATION_DOTS_PREFERENCE_KEY.equals(preference.getKey())) {
                return BuildConfig.NOTIFICATION_DOTS_ENABLED;
            }
            return true;
        }
    }
}
