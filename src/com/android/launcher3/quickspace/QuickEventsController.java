/*
 * Copyright (C) 2018 CypherOS
 * Copyright (C) 2025 DerpFest
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
package com.android.launcher3.quickspace;

import android.app.ActivityManager;
import android.os.Debug;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class QuickEventsController {

    private final Context mContext;

    private String mEventTitle;
    private String mEventTitleSub;
    private OnClickListener mEventTitleSubAction = null;
    private int mEventSubIcon;

    private int mRandomDayQuotePercent;
    private boolean mQuickEventMemoryInfo = false;
    private boolean mQuickEventAppMemoryInfo = false;

    private boolean mIsQuickEvent = false;
    private boolean mRunning = true;
    private boolean mPSAListenerRegistered = false;
    private boolean mPackageChangeListenerRegistered = false;
    private String[] mPSAEncouragedAppsStr;
    private String[] mPSADiscouragedAppsStr;

    private boolean mAppsScanned = false;
    private final boolean mDebugMode = false;

    private final Map<Integer, String[]> mCachedPSAMap = new HashMap<>();
    private final Set<String> mKnownMatchedApps = new HashSet<>();
    private final Map<String, AppType> mWatchedKeywordMap = new HashMap<>();
    private final Map<String, AppType> mMatchedAppTypes = new HashMap<>();

    private enum AppType {
        ENCOURAGED,
        DISCOURAGED
    }

    // Device Intro
    private long mInitTimestamp = 0;
    private int mIntroTimeout = 0;

    // PSA + Personality
    private String[] mPSAMorningStr;
    private String[] mPSAEvenStr;
    private String[] mPSAMidniteStr;
    private String[] mPSARandomStr;
    private String[] mPSABlazeItStr;
    private String[] mPSAEncouragedStr;
    private String[] mPSADiscouragedStr;

    private final BroadcastReceiver mPSAListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            psonalityEvent();
        }
    };

    private final BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data == null) return;
            String pkg = data.getSchemeSpecificPart();

            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                mKnownMatchedApps.remove(pkg);
                mMatchedAppTypes.remove(pkg);
            } else {
                checkAppForKeywords(pkg);
            }
        }
    };

    // NowPlaying
    private boolean mEventNowPlaying = false;
    private String mNowPlayingTitle;
    private String mNowPlayingArtist;
    private boolean mClientLost = true;
    private boolean mPlayingActive = false;

    public QuickEventsController(Context context) {
        mContext = context;
        initQuickEvents();
    }

    public void initQuickEvents() {
        mInitTimestamp = Utilities.getInitTimestamp(mContext);
        mIntroTimeout = mContext.getResources().getInteger(R.integer.config_quickSpaceIntroTimeout);
        mRandomDayQuotePercent = mContext.getResources().getInteger(R.integer.config_quickSpaceChanceOfQuoteDuringDayPercent);
        registerPSAListener();
        registerPackageChangeReceiver();

        // memory stats
        mQuickEventMemoryInfo = mContext.getResources().getBoolean(R.bool.config_quickSpaceMemoryInfo);
        mQuickEventAppMemoryInfo = mContext.getResources().getBoolean(R.bool.config_quickSpaceAppMemoryInfo);

        // keyword app classification init
        mPSAEncouragedAppsStr = getCachedArray(R.array.quickspace_psa_encouraged_apps_keywords);
        mPSADiscouragedAppsStr = getCachedArray(R.array.quickspace_psa_discouraged_apps_keywords);

        for (String keyword : mPSAEncouragedAppsStr) {
            mWatchedKeywordMap.put(keyword.toLowerCase(), AppType.ENCOURAGED);
        }
        for (String keyword : mPSADiscouragedAppsStr) {
            mWatchedKeywordMap.put(keyword.toLowerCase(), AppType.DISCOURAGED);
        }

        scanAllInstalledAppsForKeywords();
        updateQuickEvents();
    }

    private void registerPSAListener() {
        if (mPSAListenerRegistered) return;
        mPSAListenerRegistered = true;
        IntentFilter psonalityIntent = new IntentFilter();
        psonalityIntent.addAction(Intent.ACTION_TIME_TICK);
        psonalityIntent.addAction(Intent.ACTION_TIME_CHANGED);
        psonalityIntent.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        mContext.registerReceiver(mPSAListener, psonalityIntent);
    }

    private void unregisterPSAListener() {
        if (!mPSAListenerRegistered) return;
        mPSAListenerRegistered = false;
        mContext.unregisterReceiver(mPSAListener);
    }

    private void registerPackageChangeReceiver() {
        if (mPackageChangeListenerRegistered) return;
        mPackageChangeListenerRegistered = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mPackageChangeReceiver, filter);
    }

    private void unregisterPackageChangeReceiver() {
        if (mPackageChangeListenerRegistered) return;
        mPackageChangeListenerRegistered = false;
        mContext.unregisterReceiver(mPackageChangeReceiver);
    }

    private void checkAppForKeywords(String packageName) {
        if (mKnownMatchedApps.contains(packageName)) return;
        String lowerPkg = packageName.toLowerCase();
        for (Map.Entry<String, AppType> entry : mWatchedKeywordMap.entrySet()) {
            if (lowerPkg.contains(entry.getKey())) {
                mKnownMatchedApps.add(packageName);
                mMatchedAppTypes.put(packageName, entry.getValue());
                break;
            }
        }
    }

    private void scanAllInstalledAppsForKeywords() {
        if (mAppsScanned) return;
        mAppsScanned = true;

        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : apps) {
            checkAppForKeywords(appInfo.packageName);
        }
    }

    public String getMatchedKeywordApp() {
        if (mKnownMatchedApps.isEmpty()) return null;
        int index = getLuckyNumber(0, mKnownMatchedApps.size() - 1);
        return new ArrayList<>(mKnownMatchedApps).get(index);
    }

    private String getAppLabelSafe(String packageName) {
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    public void updateQuickEvents() {
        deviceIntroEvent();
        nowPlayingEvent();
        initNowPlayingEvent();
        psonalityEvent();
    }

    private void deviceIntroEvent() {
        if (!mRunning || isDeviceIntroCompleted()) return;

        mIsQuickEvent = true;
        mEventTitle = mContext.getResources().getString(R.string.quick_event_rom_intro_welcome);
        String[] intros = mContext.getResources().getStringArray(R.array.welcome_message_variants);
        mEventTitleSub = intros[getLuckyNumber(intros.length - 1)];
        mEventSubIcon = R.drawable.ic_quickspace_derp;

        mEventTitleSubAction = view -> {
            long forceComplete = Utilities.getInitTimestamp(mContext) - (mIntroTimeout * 60000);
            Utilities.setInitTimestamp(mContext, forceComplete);
            initQuickEvents();
        };
    }

    public void nowPlayingEvent() {
        if (mEventNowPlaying) {
            boolean infoExpired = !mPlayingActive || mClientLost;
            if (infoExpired) {
                mIsQuickEvent = false;
                mEventNowPlaying = false;
            }
        }
    }

    public void initNowPlayingEvent() {
        if (!mRunning || !isDeviceIntroCompleted()) return;
        if (!Utilities.isQuickspaceNowPlaying(mContext)) return;
        if (!mPlayingActive || mNowPlayingTitle == null) return;

        mEventTitle = Utilities.showDateInPlaceOfNowPlaying(mContext)
            ? Utilities.formatDateTime(mContext, System.currentTimeMillis())
            : mContext.getResources().getString(R.string.quick_event_ambient_now_playing);

        mEventTitleSub = (mNowPlayingArtist == null)
            ? mNowPlayingTitle
            : String.format(mContext.getResources().getString(R.string.quick_event_ambient_song_artist),
                        mNowPlayingTitle, mNowPlayingArtist);

        mEventSubIcon = R.drawable.ic_music_note_24dp;
        mIsQuickEvent = true;
        mEventNowPlaying = true;

        mEventTitleSubAction = view -> {
            if (mPlayingActive) {
                Intent npIntent = new Intent(Intent.ACTION_MAIN);
                npIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                npIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    Launcher.getLauncher(mContext).startActivitySafely(view, npIntent, null);
                } catch (ActivityNotFoundException ignored) {
                }
            }
        };
    }

    public void psonalityEvent() {
        if (!isDeviceIntroCompleted() || mEventNowPlaying) return;
        if (!Utilities.isQuickspacePersonalityEnabled(mContext)) return;

        mEventTitle = Utilities.formatDateTime(mContext, System.currentTimeMillis());
        mPSAMorningStr = getCachedArray(R.array.quickspace_psa_morning);
        mPSAEvenStr = getCachedArray(R.array.quickspace_psa_evening);
        mPSAMidniteStr = getCachedArray(R.array.quickspace_psa_midnight);
        mPSARandomStr = getCachedArray(R.array.quickspace_psa_random);
        mPSABlazeItStr = getCachedArray(R.array.quickspace_psa_blaze_it);

        mEventTitleSubAction = view -> { /* haha yes */ };

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        String appMessage = "§";
        
        //dedicated 420 quote
        if ((hour == 4 || hour == 16) && (minute >= 20 && minute < 30)) {
            mEventTitleSub = mPSABlazeItStr[getLuckyNumber(0, mPSABlazeItStr.length - 1)];
            mEventSubIcon = R.drawable.ic_quickspace_derp;
            mIsQuickEvent = true;
        } else switch (hour) {
            case 5: case 6: case 7: case 8: case 9:
                mEventTitleSub = mPSAMorningStr[getLuckyNumber(0, mPSAMorningStr.length - 1)];
                mEventSubIcon = R.drawable.ic_quickspace_morning;
                mIsQuickEvent = true;
                break;
            case 19: case 20: case 21: case 22: case 23:
                mEventTitleSub = mPSAEvenStr[getLuckyNumber(0, mPSAEvenStr.length - 1)];
                mEventSubIcon = R.drawable.ic_quickspace_evening;
                mIsQuickEvent = true;
                break;
            case 0: case 1: case 2: case 3: case 4:
                mEventTitleSub = mPSAMidniteStr[getLuckyNumber(0, mPSAMidniteStr.length - 1)];
                mEventSubIcon = R.drawable.ic_quickspace_midnight;
                mIsQuickEvent = true;
                break;
            default:
                int chance = (int)(ThreadLocalRandom.current().nextDouble() * 100);
                if (chance < mRandomDayQuotePercent) {
                    String detectedApp = getMatchedKeywordApp();
                    //50% chance of random day quote chance to display app quote
                    //if any app match was found, that is.
                    if (detectedApp != null && chance < mRandomDayQuotePercent / 2) {
                        String appLabel = getAppLabelSafe(detectedApp);
                        AppType type = mMatchedAppTypes.get(detectedApp);
                        switch(type) {
                            case AppType.ENCOURAGED:
                                mPSAEncouragedStr = getCachedArray(R.array.quickspace_psa_encouraged_apps_messages);
                                appMessage = mPSAEncouragedStr[getLuckyNumber(0, mPSAEncouragedStr.length - 1)];
                                mEventTitleSub = replacePlaceholderWithContent(appMessage, appLabel);
                                break;
                            case AppType.DISCOURAGED:
                                mPSADiscouragedStr = getCachedArray(R.array.quickspace_psa_discouraged_apps_messages);
                                appMessage = mPSADiscouragedStr[getLuckyNumber(0, mPSADiscouragedStr.length - 1)];
                                mEventTitleSub = replacePlaceholderWithContent(appMessage, appLabel);
                                break;
                            default:
                                //should never happen, but let's cover it anyway.
                                mEventTitleSub = "How's " + appLabel + " treating you?";
                                break;
                        }

                        // make detected apps clickable
                        mEventTitleSubAction = view -> {
                            Intent launchIntent =
                                mContext.getPackageManager().getLaunchIntentForPackage(detectedApp);
                            if (launchIntent != null) {
                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Launcher.getLauncher(mContext).startActivitySafely(view, launchIntent, null);
                            }
                        };
                        
                        if (mQuickEventAppMemoryInfo) {
                            String appMemInfo = getMemoryInfoForPackage(detectedApp);
                            mEventTitleSub += " | " + appMemInfo;
                        }
                        mEventSubIcon = R.drawable.ic_quickspace_derp;
                        mIsQuickEvent = true;
                    } else {
                        //regular random derp quote
                        mEventTitleSub = mPSARandomStr[getLuckyNumber(0, mPSARandomStr.length - 1)];
                        mEventSubIcon = R.drawable.ic_quickspace_derp;
                        mIsQuickEvent = true;
                    }
                } else {
                    mIsQuickEvent = false;
                }
                break;
        }

        if(mQuickEventMemoryInfo && mIsQuickEvent) {
            String ownMemInfo = getOwnMemoryFootprint();
            mEventTitleSub = ownMemInfo + " | " + mEventTitleSub;
        }
    }

    public boolean isQuickEvent() {
        return mIsQuickEvent;
    }

    public boolean isDeviceIntroCompleted() {
        return ((System.currentTimeMillis() - mInitTimestamp) / 60000) > mIntroTimeout;
    }

    public String getTitle() {
        return mEventTitle;
    }

    public String getActionTitle() {
        return mEventTitleSub;
    }

    public String replacePlaceholderWithContent(String message, String appLabel) {
        return message.replace("§", appLabel);
    }

    public OnClickListener getAction() {
        return mEventTitleSubAction;
    }

    public int getActionIcon() {
        return mEventSubIcon;
    }

    public int getLuckyNumber(int max) {
        return getLuckyNumber(0, max);
    }

    public int getLuckyNumber(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public void setMediaInfo(String title, String artist, boolean clientLost, boolean activePlayback) {
        mNowPlayingTitle = title;
        mNowPlayingArtist = artist;
        mClientLost = clientLost;
        mPlayingActive = activePlayback;
    }

    public void onPause() {
        mRunning = false;
        unregisterPSAListener();
        unregisterPackageChangeReceiver();
    }

    public void onResume() {
        mRunning = true;
        registerPSAListener();
        registerPackageChangeReceiver();

        //if (mQuickEventMemoryInfo) {
        //    Log.d("QuickEvents", "Current RAM: " + getOwnMemoryFootprint());
        //}
    }

    private String[] getCachedArray(int resId) {
        if (!mCachedPSAMap.containsKey(resId)) {
            mCachedPSAMap.put(resId, mContext.getResources().getStringArray(resId));
        }
        return mCachedPSAMap.get(resId);
    }

    private String getMemoryInfoForPackage(String packageName) {
        try {
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();

            for (ActivityManager.RunningAppProcessInfo procInfo : runningProcesses) {
                if (Arrays.asList(procInfo.pkgList).contains(packageName)) {
                    int[] pids = new int[]{procInfo.pid};
                    android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
                    if (memoryInfoArray.length > 0) {
                        int totalPssKb = memoryInfoArray[0].getTotalPss(); // in KB
                        double totalMb = totalPssKb / 1024.0;
                        return String.format("%.1f MB", totalMb);
                    }
                }
            }
        } catch (Exception e) {
            // ignored, fallback below
        }
        return "";
    }

    private String getOwnMemoryFootprint() {
        try {
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            int pid = android.os.Process.myPid();
            Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(new int[]{pid});
            if (memoryInfoArray.length > 0) {
                int totalPssKb = memoryInfoArray[0].getTotalPss(); // in KB
                double totalMb = totalPssKb / 1024.0;
                return String.format("%.1f MB", totalMb);
            }
        } catch (Exception e) {
            // ignored, fallback below
        }
        return "- MB";
    }
}
