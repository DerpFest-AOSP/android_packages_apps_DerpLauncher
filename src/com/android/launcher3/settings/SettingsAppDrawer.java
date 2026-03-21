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

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AppDrawerStyle;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.model.WidgetsModel;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * Settings activity for Launcher.
 */
public class SettingsAppDrawer extends CollapsingToolbarBaseActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback {

    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    @VisibleForTesting
    static final String EXTRA_FRAGMENT = ":settings:fragment";
    @VisibleForTesting
    static final String EXTRA_FRAGMENT_ARGS = ":settings:fragment_args";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Intent intent = getIntent();

        if (savedInstanceState == null) {
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            if (args == null) {
                args = new Bundle();
            }

            String prefKey = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(prefKey)) {
                args.putString(EXTRA_FRAGMENT_ARG_KEY, prefKey);
            }

            final FragmentManager fm = getSupportFragmentManager();
            final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(),
                    getString(R.string.app_drawer_settings_fragment_name));
            f.setArguments(args);
            // Display the fragment as the main content.
            fm.beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, f)
                    .commit();
        }
    }

    private boolean startPreference(String fragment, Bundle args, String key) {
        if (getSupportFragmentManager().isStateSaved()) {
            // Sometimes onClick can come after onPause because of being posted on the handler.
            // Skip starting new preferences in that case.
            return false;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), fragment);
        if (f instanceof DialogFragment) {
            f.setArguments(args);
            ((DialogFragment) f).show(fm, key);
        } else {
            startActivity(new Intent(this, SettingsAppDrawer.class)
                    .putExtra(EXTRA_FRAGMENT, fragment)
                    .putExtra(EXTRA_FRAGMENT_ARGS, args));
        }
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragmentCompat preferenceFragment, Preference pref) {
        return startPreference(pref.getFragment(), pref.getExtras(), pref.getKey());
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        return startPreference(getString(R.string.app_drawer_category_title), args, pref.getKey());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            theme.applyStyle(
                    com.android.settingslib.widget.theme.R.style.Theme_SubSettingsBase_Expressive,
                    true);
        }
        return theme;
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class AppDrawerSettingsFragment extends SettingsBasePreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;
        private Preference mThemeAllAppsIconsPref;

        private static final String KEY_OPEN_KEYBOARD = "pref_drawer_open_keyboard";
        private static final String KEY_APP_DRAWER_STYLE = "pref_app_drawer_style";

        private ListPreference mSearchPlacementPref;
        private ListPreference mDrawerStylePref;
        private Preference mDrawerListPref;
        private Preference mOpenKeyboardPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Bundle args = getArguments();
            mHighLightKey = args == null ? null : args.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (rootKey == null && !TextUtils.isEmpty(mHighLightKey)) {
                rootKey = getParentKeyForPref(mHighLightKey);
            }

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_app_drawer_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (preference instanceof PreferenceCategory) {
                    PreferenceCategory category = (PreferenceCategory) preference;
                    for (int j = category.getPreferenceCount() - 1; j >= 0; j--) {
                        Preference innerPref = category.getPreference(j);
                        if (!initPreference(innerPref)) {
                            category.removePreference(innerPref);
                        }
                    }
                    continue;
                }
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }

            if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }

            mSearchPlacementPref = (ListPreference) screen.findPreference(
                    LauncherPrefs.ALL_APPS_SEARCH_PLACEMENT.getSharedPrefKey());
            mDrawerStylePref = (ListPreference) screen.findPreference(KEY_APP_DRAWER_STYLE);
            mDrawerListPref = screen.findPreference(LauncherPrefs.DRAWER_LIST.getSharedPrefKey());
            mOpenKeyboardPref = screen.findPreference(KEY_OPEN_KEYBOARD);
            updateOpenKeyboardEnabled();
            ensureDrawerCaddyPrefsConsistent();
            if (mDrawerStylePref != null) {
                mDrawerStylePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (!AppDrawerStyle.NORMAL.equals(String.valueOf(newValue))) {
                        LauncherPrefs.INSTANCE.get(requireContext()).put(LauncherPrefs.DRAWER_LIST, true);
                    }
                    return true;
                });
            }
            if (mDrawerListPref != null) {
                mDrawerListPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean defaultList = (Boolean) newValue;
                    if (!defaultList) {
                        if (!AppDrawerStyle.isIos(LauncherPrefs.INSTANCE.get(requireContext())
                                .get(LauncherPrefs.APP_DRAWER_STYLE))) {
                            LauncherPrefs.INSTANCE.get(requireContext()).put(
                                    LauncherPrefs.APP_DRAWER_STYLE, AppDrawerStyle.NORMAL);
                            if (mDrawerStylePref != null) {
                                mDrawerStylePref.setValue(AppDrawerStyle.NORMAL);
                            }
                        }
                    }
                    return true;
                });
            }
            updateDrawerCaddyRestrictionUi();

            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            View listView = getListView();
            final int bottomPadding = listView.getPaddingBottom();
            listView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bottomPadding + insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
        }

        @Override
        public void onDestroy() {
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (LauncherPrefs.ALL_APPS_SEARCH_PLACEMENT.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_STYLE.getSharedPrefKey().equals(key)) {
                updateOpenKeyboardEnabled();
            }
            if (LauncherPrefs.ALL_APPS_SEARCH_PLACEMENT.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_STYLE.getSharedPrefKey().equals(key)
                    || LauncherPrefs.DRAWER_LIST.getSharedPrefKey().equals(key)) {
                updateDrawerCaddyRestrictionUi();
            }
            if (LauncherPrefs.ALL_APPS_SEARCH_PLACEMENT.getSharedPrefKey().equals(key)) {
                try {
                    LauncherAppState appState = LauncherAppState.getInstance(getContext());
                    appState.getModel().rebindCallbacks();
                } catch (Exception e) {
                    LauncherAppState.INSTANCE.get(getContext()).setNeedsRestart();
                }
            }
            if (LauncherPrefs.DRAWER_LIST.getSharedPrefKey().equals(key)) {
                // Trigger a refresh of the app list without requiring a restart
                // This will cause onAppsUpdated() to be called, which will recategorize apps
                try {
                    LauncherAppState appState = LauncherAppState.getInstance(getContext());
                    appState.getModel().rebindCallbacks();
                } catch (Exception e) {
                    // Fallback to restart if rebind fails
                    LauncherAppState.INSTANCE.get(getContext()).setNeedsRestart();
                }
            }
            if (LauncherPrefs.ALL_APPS_DARK_TEXT.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_STYLE.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_CUSTOM_COLOR_ENABLED.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_CUSTOM_COLOR_LIGHT.getSharedPrefKey().equals(key)
                    || LauncherPrefs.APP_DRAWER_CUSTOM_COLOR_DARK.getSharedPrefKey().equals(key)) {
                LauncherAppState.INSTANCE.get(getContext()).setNeedsRestart();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        protected String getParentKeyForPref(String key) {
            return null;
        }

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        protected boolean initPreference(Preference preference) {
            switch (preference.getKey()) {
                case "pref_allapps_themed_icons":
                    mThemeAllAppsIconsPref = preference;
                    updateThemeAllAppsIconsPref();
                    break;
            }

            return true;
        }

        @Override
        public void onResume() {
            super.onResume();

            if (isAdded() && !mPreferenceHighlighted) {
                PreferenceHighlighter highlighter = createHighlighter();
                if (highlighter != null) {
                    getView().postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS);
                    mPreferenceHighlighted = true;
                } else {
                    requestAccessibilityFocus(getListView());
                }
            }

            if (mThemeAllAppsIconsPref != null) {
                updateThemeAllAppsIconsPref();
            }
            ensureDrawerCaddyPrefsConsistent();
            updateDrawerCaddyRestrictionUi();
        }

        private void updateThemeAllAppsIconsPref() {
            boolean enabled = ThemeManager.INSTANCE.get(getContext()).isMonoThemeEnabled();
            mThemeAllAppsIconsPref.setEnabled(enabled);
            mThemeAllAppsIconsPref.setSummary(getContext().getString(enabled
                    ? R.string.pref_themed_icons_summary
                    : R.string.themed_icons_disabled_summary));
        }

        private void updateOpenKeyboardEnabled() {
            if (mOpenKeyboardPref == null || mSearchPlacementPref == null) return;
            boolean searchVisible = !"hidden".equals(mSearchPlacementPref.getValue());
            String style = mDrawerStylePref == null
                    ? AppDrawerStyle.NORMAL : mDrawerStylePref.getValue();
            mOpenKeyboardPref.setEnabled(searchVisible && !AppDrawerStyle.isIos(style));
        }

        /** Aligns stored drawer style with Caddy rules and refreshes enabled state / summaries. */
        private void ensureDrawerCaddyPrefsConsistent() {
            Context ctx = getContext();
            if (ctx == null) {
                return;
            }
            boolean defaultList = LauncherPrefs.INSTANCE.get(ctx).get(LauncherPrefs.DRAWER_LIST);
            String storedStyle = LauncherPrefs.INSTANCE.get(ctx).get(LauncherPrefs.APP_DRAWER_STYLE);
            if (!defaultList && !AppDrawerStyle.NORMAL.equals(storedStyle)
                    && !AppDrawerStyle.isIos(storedStyle)) {
                LauncherPrefs.INSTANCE.get(ctx).put(LauncherPrefs.APP_DRAWER_STYLE, AppDrawerStyle.NORMAL);
                if (mDrawerStylePref != null) {
                    mDrawerStylePref.setValue(AppDrawerStyle.NORMAL);
                }
            }
        }

        private void updateDrawerCaddyRestrictionUi() {
            Context ctx = getContext();
            if (ctx == null) {
                return;
            }
            boolean defaultList = LauncherPrefs.INSTANCE.get(ctx).get(LauncherPrefs.DRAWER_LIST);
            String storedStyle = LauncherPrefs.INSTANCE.get(ctx).get(LauncherPrefs.APP_DRAWER_STYLE);
            boolean nonNormal = !AppDrawerStyle.NORMAL.equals(storedStyle);

            if (mDrawerStylePref != null) {
                final boolean ios = AppDrawerStyle.isIos(storedStyle);
                mDrawerStylePref.setEnabled(defaultList || ios);
                if (ios) {
                    mDrawerStylePref.setSummary(ctx.getString(R.string.drawer_style_summary_ios_forced));
                } else if (!defaultList) {
                    mDrawerStylePref.setSummary(ctx.getString(R.string.drawer_style_unavailable_with_caddy));
                } else {
                    mDrawerStylePref.setSummary(mDrawerStylePref.getEntry());
                }
            }
            if (mDrawerListPref != null) {
                mDrawerListPref.setEnabled(!nonNormal);
                if (nonNormal) {
                    mDrawerListPref.setSummary(ctx.getString(R.string.drawer_list_unavailable_with_special_drawer));
                } else {
                    mDrawerListPref.setSummary(ctx.getString(R.string.drawer_list_summary));
                }
            }
        }

        private PreferenceHighlighter createHighlighter() {
            if (TextUtils.isEmpty(mHighLightKey)) {
                return null;
            }

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                return null;
            }

            RecyclerView list = getListView();
            PreferencePositionCallback callback = (PreferencePositionCallback) list.getAdapter();
            int position = callback.getPreferenceAdapterPosition(mHighLightKey);
            return position >= 0 ? new PreferenceHighlighter(
                    list, position, screen.findPreference(mHighLightKey))
                    : null;
        }

        private void requestAccessibilityFocus(@NonNull final RecyclerView rv) {
            rv.post(() -> {
                if (!rv.hasFocus() && rv.getChildCount() > 0) {
                    rv.getChildAt(0)
                            .performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
                }
            });
        }
    }
}
