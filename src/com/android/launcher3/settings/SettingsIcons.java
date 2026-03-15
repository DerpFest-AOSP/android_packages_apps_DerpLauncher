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

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.SettingsCache.NOTIFICATION_BADGING_URI;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.R;
import com.android.launcher3.util.SafeCloseable;
import com.android.launcher3.util.SettingsCache;

import androidx.preference.Preference;

import kotlin.Unit;

/**
 * Settings activity for icon preferences.
 */
public class SettingsIcons extends SettingsCategoryActivity {

    private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";
    private static final String KEY_NOTIFICATION_BADGE_COUNTS = "pref_notification_badge_counts";

    @Override
    protected String getSettingsFragmentName() {
        return getString(R.string.icons_settings_fragment_name);
    }

    public static class IconsSettingsFragment extends CategorySettingsFragment {

        private Preference mBadgeCountsPref;
        private SafeCloseable mDotsListener;

        @Override
        protected int getPreferencesXmlResId() {
            return R.xml.launcher_icons_preferences;
        }

        @Override
        protected boolean initPreference(Preference preference) {
            if (NOTIFICATION_DOTS_PREFERENCE_KEY.equals(preference.getKey())) {
                return BuildConfig.NOTIFICATION_DOTS_ENABLED;
            }
            if (KEY_NOTIFICATION_BADGE_COUNTS.equals(preference.getKey())) {
                mBadgeCountsPref = preference;
                updateBadgeCountsPref(SettingsCache.INSTANCE.get(getContext())
                        .getValue(NOTIFICATION_BADGING_URI));
                return BuildConfig.NOTIFICATION_DOTS_ENABLED;
            }
            return true;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (mBadgeCountsPref != null) {
                mDotsListener = SettingsCache.INSTANCE.get(requireContext())
                        .getListenableRef(NOTIFICATION_BADGING_URI)
                        .forEach(MAIN_EXECUTOR, this::updateBadgeCountsPref);
            }
        }

        @Override
        public void onStop() {
            if (mDotsListener != null) {
                mDotsListener.close();
                mDotsListener = null;
            }
            super.onStop();
        }

        private Unit updateBadgeCountsPref(boolean dotsEnabled) {
            if (mBadgeCountsPref == null) {
                return null;
            }
            mBadgeCountsPref.setEnabled(dotsEnabled);
            mBadgeCountsPref.setSummary(dotsEnabled
                    ? R.string.notification_badge_counts_summary
                    : R.string.notification_badge_counts_disabled_summary);
            return null;
        }
    }
}
