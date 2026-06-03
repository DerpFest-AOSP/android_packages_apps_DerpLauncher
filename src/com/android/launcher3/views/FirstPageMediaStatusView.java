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

package com.android.launcher3.views;

import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.R;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Themes;

/**
 * Media page shown alongside the compact first-page status strip.
 */
public class FirstPageMediaStatusView extends FrameLayout {

    private final int mWorkspaceTextColor;
    private final int mFallbackIconPadding;
    private final int mArtworkCornerRadius;
    private Drawable mFallbackIcon;

    private View mArtworkContainer;
    private ImageView mIconView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    @Nullable
    private ApiWrapper.MediaInfo mMediaInfo;

    public FirstPageMediaStatusView(Context context) {
        this(context, null);
    }

    public FirstPageMediaStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FirstPageMediaStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.first_page_status_media, this, true);
        mWorkspaceTextColor = Themes.getAttrColor(context, R.attr.workspaceTextColor);
        mFallbackIconPadding = getResources().getDimensionPixelSize(
                R.dimen.first_page_status_media_icon_inner_padding);
        mArtworkCornerRadius = getResources().getDimensionPixelSize(
                R.dimen.first_page_status_media_icon_radius);
        mFallbackIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_first_page_media);
        bindViews();
        updateViews();
    }

    public void setMediaInfo(@Nullable ApiWrapper.MediaInfo mediaInfo) {
        mMediaInfo = mediaInfo;
        updateViews();
    }

    public boolean hasMedia() {
        return mMediaInfo != null && !TextUtils.isEmpty(mMediaInfo.getTitle());
    }

    private void bindViews() {
        mArtworkContainer = findViewById(R.id.first_page_status_media_artwork_container);
        mArtworkContainer.setClipToOutline(true);
        mArtworkContainer.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mArtworkCornerRadius);
            }
        });
        mIconView = findViewById(R.id.first_page_status_media_icon);
        mTitleView = findViewById(R.id.first_page_status_media_title);
        mSubtitleView = findViewById(R.id.first_page_status_media_subtitle);
    }

    private void updateViews() {
        if (!hasMedia()) {
            mTitleView.setText(null);
            mSubtitleView.setText(null);
            mSubtitleView.setVisibility(GONE);
            mIconView.setImageDrawable(mFallbackIcon);
            mIconView.setImageTintList(ColorStateList.valueOf(mWorkspaceTextColor));
            mArtworkContainer.setSelected(false);
            return;
        }

        Drawable icon = mMediaInfo.getIcon();
        if (icon == null) {
            icon = mFallbackIcon;
        }
        boolean hasArtwork = icon != mFallbackIcon;
        mIconView.setImageDrawable(icon);
        mIconView.setImageTintList(
                !hasArtwork
                        ? ColorStateList.valueOf(mWorkspaceTextColor)
                        : null);
        int padding = hasArtwork ? 0 : mFallbackIconPadding;
        mIconView.setPadding(padding, padding, padding, padding);
        mIconView.setScaleType(hasArtwork ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        mArtworkContainer.setSelected(hasArtwork);
        mTitleView.setText(mMediaInfo.getTitle());

        CharSequence subtitle = mMediaInfo.getSubtitle();
        if (TextUtils.isEmpty(subtitle)) {
            mSubtitleView.setText(null);
            mSubtitleView.setVisibility(GONE);
        } else {
            mSubtitleView.setText(subtitle);
            mSubtitleView.setVisibility(VISIBLE);
        }
    }
}
