/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.settings;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.R;

/**
 * Homepage logo card. Shows the current wallpaper behind the mark, without a maintainer label.
 */
public class SettingsHeaderPreference extends Preference {

    public static final String KEY = "derp_settings_header";

    private static final String TAG = "SettingsHeader";
    private static final int CAPTURE_ATTEMPTS = 20;
    private static final long CAPTURE_INTERVAL_MS = 150L;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver mWallpaperReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshWallpaper();
        }
    };

    @Nullable private ImageView mWallpaperView;
    @Nullable private Drawable mDrawable;
    @Nullable private SettingsHeaderWallpaper.Layer mWallpaperLayer;
    private int mGeneration;
    private boolean mListening;
    private boolean mLoaded;

    public SettingsHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSelectable(false);
    }

    public SettingsHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSelectable(false);
    }

    public SettingsHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
    }

    public SettingsHeaderPreference(Context context) {
        super(context);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mWallpaperView = (ImageView) holder.findViewById(R.id.settings_header_wallpaper);
        if (mLoaded) {
            publish(mDrawable);
        }
    }

    /** Starts wallpaper loading while the settings screen is visible. */
    public void onHostResume() {
        Context context = getContext();
        if (!mListening && context != null) {
            ContextCompat.registerReceiver(context, mWallpaperReceiver,
                    new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED);
            mListening = true;
        }
        refreshWallpaper();
    }

    /** Stops wallpaper updates and hides the live wallpaper layer. */
    public void onHostPause() {
        mGeneration++;
        mHandler.removeCallbacksAndMessages(null);
        closeWallpaperLayer();
        if (mListening) {
            Context context = getContext();
            if (context != null) {
                context.unregisterReceiver(mWallpaperReceiver);
            }
            mListening = false;
        }
    }

    private void refreshWallpaper() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final int generation = ++mGeneration;
        closeWallpaperLayer();
        mHandler.removeCallbacksAndMessages(null);
        UI_HELPER_EXECUTOR.execute(() -> {
            SettingsHeaderWallpaper.Source source;
            try {
                source = SettingsHeaderWallpaper.load(context);
            } catch (RuntimeException exception) {
                Log.w(TAG, "Unable to load the header wallpaper", exception);
                source = new SettingsHeaderWallpaper.Source(false, null);
            }
            SettingsHeaderWallpaper.Source loaded = source;
            MAIN_EXECUTOR.execute(() -> applySource(generation, loaded));
        });
    }

    private void applySource(int generation, SettingsHeaderWallpaper.Source source) {
        if (generation != mGeneration) {
            return;
        }
        mLoaded = true;
        publish(source.drawable);
        if (source.captureLiveFrame) {
            startLiveCapture(generation);
        }
    }

    private void startLiveCapture(int generation) {
        Activity activity = SettingsHeaderWallpaper.findActivity(getContext());
        if (activity == null) {
            return;
        }
        mWallpaperLayer = new SettingsHeaderWallpaper.Layer(activity.getWindow());
        scheduleCapture(generation, 0);
    }

    private void scheduleCapture(int generation, int attempt) {
        mHandler.postDelayed(() -> {
            if (generation != mGeneration) {
                return;
            }
            UI_HELPER_EXECUTOR.execute(() -> {
                Bitmap bitmap = SettingsHeaderWallpaper.captureFrame();
                MAIN_EXECUTOR.execute(() -> onCapturedFrame(generation, attempt, bitmap));
            });
        }, CAPTURE_INTERVAL_MS);
    }

    private void onCapturedFrame(int generation, int attempt, @Nullable Bitmap bitmap) {
        if (generation != mGeneration) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            return;
        }
        if (bitmap != null) {
            Context context = getContext();
            if (context != null) {
                publish(new BitmapDrawable(context.getResources(), bitmap));
            } else {
                bitmap.recycle();
            }
            closeWallpaperLayer();
            return;
        }
        if (attempt + 1 < CAPTURE_ATTEMPTS) {
            scheduleCapture(generation, attempt + 1);
        } else {
            closeWallpaperLayer();
        }
    }

    private void publish(@Nullable Drawable drawable) {
        mDrawable = drawable;
        ImageView wallpaper = mWallpaperView;
        if (wallpaper == null) {
            return;
        }
        wallpaper.setImageDrawable(drawable);
        applyWallpaperBlur(wallpaper, drawable != null);
        updateMark(wallpaper, drawable != null);
    }

    private void applyWallpaperBlur(ImageView wallpaper, boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (!enabled) {
            wallpaper.setRenderEffect(null);
            return;
        }
        Context context = wallpaper.getContext();
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, night ? 8f : 14f,
                context.getResources().getDisplayMetrics());
        wallpaper.setRenderEffect(RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP));
    }

    private void updateMark(ImageView wallpaper, boolean onWallpaper) {
        if (!(wallpaper.getParent() instanceof View)) {
            return;
        }
        ImageView mark = ((View) wallpaper.getParent()).findViewById(R.id.derp_branding_image);
        if (mark == null) {
            return;
        }
        int color;
        if (onWallpaper) {
            color = ContextCompat.getColor(wallpaper.getContext(), R.color.settings_header_mark);
        } else {
            TypedValue value = new TypedValue();
            wallpaper.getContext().getTheme().resolveAttribute(
                    android.R.attr.colorAccent, value, true);
            color = value.data;
        }
        mark.setImageTintList(ColorStateList.valueOf(color));
    }

    private void closeWallpaperLayer() {
        if (mWallpaperLayer != null) {
            mWallpaperLayer.close();
            mWallpaperLayer = null;
        }
    }
}
