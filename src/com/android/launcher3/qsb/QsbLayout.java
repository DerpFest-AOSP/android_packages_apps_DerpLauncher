/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3.qsb;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Reorderable;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.util.MultiTranslateDelegate;

/**
 * Bottom hotseat QSB styled close to Pixel Launcher.
 */
public class QsbLayout extends FrameLayout implements Reorderable {

    private static final String LENS_URI = "google://lens";

    private final MultiTranslateDelegate mTranslateDelegate = new MultiTranslateDelegate(this);
    private final ThemeManager.ThemeChangeListener mThemeChangeListener = this::updateIcons;
    private float mScaleForReorderBounce = 1f;
    private ThemeManager mThemeManager;

    private ImageView mGIcon;
    private ImageView mMicIcon;
    private ImageButton mLensIcon;
    private ImageView mAiModeButton;

    public QsbLayout(Context context) {
        this(context, null);
    }

    public QsbLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mThemeManager = ThemeManager.INSTANCE.get(getContext());
        mGIcon = findViewById(R.id.g_icon);
        mMicIcon = findViewById(R.id.mic_icon);
        mLensIcon = findViewById(R.id.lens_icon);
        mAiModeButton = findViewById(R.id.ai_mode_button);

        setOnClickListener(v -> Launcher.getLauncher(getContext()).startSearch(
                "", false, null, true));

        if (mLensIcon != null) {
            boolean hasLens = isLensAvailable(getContext());
            mLensIcon.setVisibility(hasLens ? VISIBLE : GONE);
            if (hasLens) {
                Intent lensIntent = getLensIntent(getContext());
                mLensIcon.setOnClickListener(v -> launchSafely(lensIntent));
            }
        }
        updateIcons();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mThemeManager != null) {
            mThemeManager.addChangeListener(mThemeChangeListener);
        }
        updateIcons();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mThemeManager != null) {
            mThemeManager.removeChangeListener(mThemeChangeListener);
        }
        super.onDetachedFromWindow();
    }

    private void updateIcons() {
        boolean themedIconsEnabled = mThemeManager != null && mThemeManager.isIconThemeEnabled();
        if (mGIcon != null) {
            mGIcon.setImageResource(themedIconsEnabled
                    ? R.drawable.ic_super_g_themed
                    : R.drawable.ic_super_g_color);
        }
        if (mMicIcon != null) {
            mMicIcon.setImageResource(themedIconsEnabled
                    ? R.drawable.ic_mic_themed
                    : R.drawable.ic_mic_color);
        }
        if (mLensIcon != null) {
            mLensIcon.setImageResource(themedIconsEnabled
                    ? R.drawable.ic_lens_themed
                    : R.drawable.ic_lens_color);
        }
        if (mAiModeButton != null) {
            mAiModeButton.setImageResource(themedIconsEnabled
                    ? R.drawable.ic_ai_mode_themed
                    : R.drawable.ic_ai_mode_color);
        }
    }

    private void launchSafely(Intent intent) {
        try {
            getContext().startActivity(intent);
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
        }
        // Fallback: try ACTION_WEB_SEARCH
        try {
            Intent search = new Intent(Intent.ACTION_WEB_SEARCH)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(search);
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
        }
        // Last fallback: open Google app
        try {
            Intent launch = getContext().getPackageManager()
                    .getLaunchIntentForPackage(com.android.launcher3.Utilities.GSA_PACKAGE);
            if (launch != null) {
                getContext().startActivity(launch);
                return;
            }
        } catch (ActivityNotFoundException | SecurityException ignored) {
        }
        Launcher.getLauncher(getContext()).startSearch("", false, null, true);
    }

    static Intent getLensIntent(Context context) {
        // Primary: launch Lens activity directly by component
        Intent intent = new Intent()
                .setComponent(new android.content.ComponentName(
                        com.android.launcher3.Utilities.GSA_PACKAGE,
                        com.android.launcher3.Utilities.LENS_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            return intent;
        }
        // Fallback: try google://lens URI
        Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LENS_URI))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .setPackage(com.android.launcher3.Utilities.GSA_PACKAGE);
        if (uriIntent.resolveActivity(context.getPackageManager()) != null) {
            return uriIntent;
        }
        // Last fallback: launch Google app
        return context.getPackageManager().getLaunchIntentForPackage(
                com.android.launcher3.Utilities.GSA_PACKAGE);
    }

    static boolean isLensAvailable(Context context) {
        return com.android.launcher3.Utilities.isPackageEnabled(
                com.android.launcher3.Utilities.GSA_PACKAGE, context);
    }

    @Nullable
    static String getSearchPackage(Context context) {
        if (Utilities.isGSAEnabled(context)) {
            return Utilities.GSA_PACKAGE;
        }
        return null;
    }

    @Override
    public MultiTranslateDelegate getTranslateDelegate() {
        return mTranslateDelegate;
    }

    @Override
    public void setReorderBounceScale(float scale) {
        mScaleForReorderBounce = scale;
        super.setScaleX(scale);
        super.setScaleY(scale);
    }

    @Override
    public float getReorderBounceScale() {
        return mScaleForReorderBounce;
    }
}
