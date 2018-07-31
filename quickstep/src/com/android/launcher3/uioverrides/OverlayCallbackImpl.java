/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.uioverrides;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.systemui.plugins.shared.LauncherOverlayManager;
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks;
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayTouchProxy;

import com.google.android.libraries.gsa.launcherclient.LauncherClient;
import com.google.android.libraries.gsa.launcherclient.LauncherClientCallbacks;

import java.io.PrintWriter;

/**
 * Implements {@link LauncherOverlayTouchProxy} and passes all the corresponding events to {@link
 * LauncherClient}. {@see setClient}
 *
 * <p>Implements {@link LauncherClientCallbacks} and sends all the corresponding callbacks to {@link
 * Launcher}.
 */
public class OverlayCallbackImpl
        implements LauncherClientCallbacks, LauncherOverlayManager, LauncherOverlayTouchProxy,
        OnSharedPreferenceChangeListener, OnDeviceProfileChangeListener,
        Application.ActivityLifecycleCallbacks, View.OnAttachStateChangeListener {

    private final Launcher mLauncher;
    private final LauncherClient mClient;

    private LauncherOverlayCallbacks mLauncherOverlayCallbacks;
    private boolean mWasOverlayAttached = false;

    public OverlayCallbackImpl(Launcher launcher) {
        mLauncher = launcher;
        SharedPreferences prefs = LauncherPrefs.getPrefs(launcher);
        mClient = new LauncherClient(mLauncher, this, getClientOptions());
        mLauncher.setLauncherOverlay(this);

        prefs.registerOnSharedPreferenceChangeListener(this);
        mLauncher.addOnDeviceProfileChangeListener(this);
        mLauncher.registerActivityLifecycleCallbacks(this);
        mLauncher.getWindow().getDecorView().addOnAttachStateChangeListener(this);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        mClient.reattachOverlay();
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        mClient.onAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        mClient.onDetachedFromWindow();
    }

    @Override
    public void dump(String prefix, PrintWriter w) {
        mClient.dump(prefix, w);
    }

    @Override
    public void openOverlay() {
        mClient.showOverlay(true);
    }

    @Override
    public void hideOverlay(boolean animate) {
        mClient.hideOverlay(animate);
    }

    @Override
    public void hideOverlay(int duration) {
        mClient.hideOverlay(duration);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) { }

    @Override
    public void onActivityDestroyed(Activity activity) { }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity == mLauncher) {
            mClient.onPause();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity == mLauncher) {
            mClient.onResume();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activity == mLauncher) {
            mClient.onStart();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity == mLauncher) {
            mClient.onStop();
        }
    }

    @Override
    public void onFlingVelocity(float velocity) { }

    @Override
    public void onOverlayMotionEvent(MotionEvent ev, float scrollProgress) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> mClient.startMove();
            case MotionEvent.ACTION_MOVE -> mClient.updateMove(scrollProgress);
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mClient.endMove();
        }
    }

    @Override
    public void onActivityDestroyed() {
        mClient.onDestroy();
        mLauncher.setLauncherOverlay(null);
        LauncherPrefs.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
        mLauncher.removeOnDeviceProfileChangeListener(this);
        mLauncher.unregisterActivityLifecycleCallbacks(this);
        mLauncher.getWindow().getDecorView().removeOnAttachStateChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (LauncherPrefs.ENABLE_MINUS_ONE.getSharedPrefKey().equals(key)) {
            mClient.setClientOptions(getClientOptions());
        }
    }

    @Override
    public void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive) {
        if (overlayAttached != mWasOverlayAttached) {
            mWasOverlayAttached = overlayAttached;
            mLauncher.setLauncherOverlay(overlayAttached ? this : null);
        }
    }

    @Override
    public void onOverlayScrollChanged(float progress) {
        if (mLauncherOverlayCallbacks != null) {
            mLauncherOverlayCallbacks.onOverlayScrollChanged(progress);
        }
    }

    @Override
    public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks) {
        mLauncherOverlayCallbacks = callbacks;
    }

    private LauncherClient.ClientOptions getClientOptions() {
        return new LauncherClient.ClientOptions(
                LauncherPrefs.ENABLE_MINUS_ONE.get(mLauncher),
                true, /* enableHotword */
                true /* enablePrewarming */
        );
    }
}
