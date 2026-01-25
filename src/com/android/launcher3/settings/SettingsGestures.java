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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * Settings activity for Launcher Gestures.
 */
public class SettingsGestures extends CollapsingToolbarBaseActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback {

    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    public static final String KEY_HOMESCREEN_DT_GESTURES = "pref_homescreen_dt_gestures";
    public static final String KEY_HOMESCREEN_SWIPE_DOWN_GESTURES = "pref_homescreen_swipe_down_gestures";
    public static final String KEY_SWIPE_DOWN_SIDE = "pref_swipe_down_side";

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
                    getString(R.string.gesture_settings_fragment_name));
            f.setArguments(args);
            // Display the fragment as the main content.
            fm.beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, f)
                    .commit();
        }
    }

    private boolean startPreference(String fragment, Bundle args, String key) {
        if (Utilities.ATLEAST_T && getSupportFragmentManager().isStateSaved()) {
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
            startActivity(new Intent(this, SettingsGestures.class)
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
        return startPreference(getString(R.string.home_category_title), args, pref.getKey());
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
     * This fragment shows the gesture preferences.
     */
    public static class GestureSettingsFragment extends SettingsBasePreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;

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
            setPreferencesFromResource(R.xml.launcher_gesture_preferences, rootKey);

            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (!initPreference(preference)) {
                    screen.removePreference(preference);
                }
            }

            final ListPreference doubletabAction = (ListPreference) findPreference(KEY_HOMESCREEN_DT_GESTURES);
            if (doubletabAction != null) {
                SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                doubletabAction.setValue(devicePrefs.getString(KEY_HOMESCREEN_DT_GESTURES, "1"));
                doubletabAction.setSummary(doubletabAction.getEntry());
                doubletabAction.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String dtGestureValue = (String) newValue;
                        SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                        devicePrefs.edit().putString(KEY_HOMESCREEN_DT_GESTURES, dtGestureValue).commit();
                        doubletabAction.setValue(dtGestureValue);
                        doubletabAction.setSummary(doubletabAction.getEntry());
                        updateDoubleTapDependentPreferences(dtGestureValue);
                        Toast.makeText(getActivity(), R.string.restarting_launcher_changes, Toast.LENGTH_SHORT).show();
                        Utilities.restartLauncher(getActivity());
                        return true;
                    }
                });
            }

            final ListPreference swipeDownAction = (ListPreference) findPreference(KEY_HOMESCREEN_SWIPE_DOWN_GESTURES);
            if (swipeDownAction != null) {
                SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                swipeDownAction.setValue(devicePrefs.getString(KEY_HOMESCREEN_SWIPE_DOWN_GESTURES, "0"));
                swipeDownAction.setSummary(swipeDownAction.getEntry());
                swipeDownAction.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String swipeDownGestureValue = (String) newValue;
                        SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                        devicePrefs.edit().putString(KEY_HOMESCREEN_SWIPE_DOWN_GESTURES, swipeDownGestureValue).commit();
                        swipeDownAction.setValue(swipeDownGestureValue);
                        swipeDownAction.setSummary(swipeDownAction.getEntry());
                        updateSwipeDownDependentPreferences(swipeDownGestureValue);
                        Toast.makeText(getActivity(), R.string.restarting_launcher_changes, Toast.LENGTH_SHORT).show();
                        Utilities.restartLauncher(getActivity());
                        return true;
                    }
                });
            }

            final ListPreference swipeDownSide = (ListPreference) findPreference(KEY_SWIPE_DOWN_SIDE);
            if (swipeDownSide != null) {
                SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                swipeDownSide.setValue(devicePrefs.getString(KEY_SWIPE_DOWN_SIDE, "0"));
                swipeDownSide.setSummary(swipeDownSide.getEntry());
                swipeDownSide.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String swipeDownSideValue = (String) newValue;
                        SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
                        devicePrefs.edit().putString(KEY_SWIPE_DOWN_SIDE, swipeDownSideValue).commit();
                        swipeDownSide.setValue(swipeDownSideValue);
                        swipeDownSide.setSummary(swipeDownSide.getEntry());
                        Toast.makeText(getActivity(), R.string.restarting_launcher_changes, Toast.LENGTH_SHORT).show();
                        Utilities.restartLauncher(getActivity());
                        return true;
                    }
                });
            }

            if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }
            
            // Initialize enabled state of dependent preferences
            SharedPreferences devicePrefs = getActivity().getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE);
            String dtGestureValue = devicePrefs.getString(KEY_HOMESCREEN_DT_GESTURES, "1");
            String swipeDownGestureValue = devicePrefs.getString(KEY_HOMESCREEN_SWIPE_DOWN_GESTURES, "0");
            updateDoubleTapDependentPreferences(dtGestureValue);
            updateSwipeDownDependentPreferences(swipeDownGestureValue);
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
            // Restart is now handled by Launcher activity
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

        /**
         * Updates enabled state of double tap gesture dependent preferences
         */
        private void updateDoubleTapDependentPreferences(String gestureValue) {
            Preference hapticsPref = findPreference("pref_haptics_on_dt_gestures");
            if (hapticsPref != null) {
                boolean isGestureEnabled = !"0".equals(gestureValue);
                hapticsPref.setEnabled(isGestureEnabled);
            }
        }

        /**
         * Updates enabled state of swipe down gesture dependent preferences
         */
        private void updateSwipeDownDependentPreferences(String gestureValue) {
            Preference hapticsPref = findPreference("pref_haptics_on_swipe_down_gestures");
            Preference sidePref = findPreference("pref_swipe_down_side");
            
            boolean isGestureEnabled = !"0".equals(gestureValue);
            
            if (hapticsPref != null) {
                hapticsPref.setEnabled(isGestureEnabled);
            }
            if (sidePref != null) {
                sidePref.setEnabled(isGestureEnabled);
            }
        }
    }
}
