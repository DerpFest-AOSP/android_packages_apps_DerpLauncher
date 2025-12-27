/*
 * Copyright (C) 2018 CypherOS
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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;

import com.android.launcher3.quickspace.QuickspaceController.OnDataListener;
import com.android.launcher3.quickspace.receivers.QuickSpaceActionReceiver;
import com.android.launcher3.quickspace.views.DateTextView;

import io.chaldeaprjkt.seraphixgoogle.Card;
import io.chaldeaprjkt.seraphixgoogle.DataProviderListener;
import io.chaldeaprjkt.seraphixgoogle.SeraphixDataProvider;

import android.util.Log;

public class QuickSpaceView extends FrameLayout implements OnDataListener {

    private static final String TAG = "Launcher3:QuickSpaceView";
    private static final boolean DEBUG = false;

    private DateTextView mTitle;
    private ViewGroup mEventContainer;
    private ImageView mEventIcon;
    private TextView mEventText;
    private TextView mSeparator;
    private ViewGroup mWeatherContainer;
    private ImageView mWeatherIcon;
    private TextView mWeatherTemp;

    private boolean mIsQuickEvent;
    private boolean mFinishedInflate;
    private boolean mWeatherAvailable;

    private QuickSpaceActionReceiver mActionReceiver;
    private QuickspaceController mController;
    private SeraphixDataProvider mSeraphixDataProvider;

    public QuickSpaceView(Context context, AttributeSet set) {
        super(context, set);
        mActionReceiver = new QuickSpaceActionReceiver(context);
        mController = new QuickspaceController(context);
        setClipChildren(false);
        mSeraphixDataProvider = new SeraphixDataProvider(context, 1022, Utilities.getSeraphixHolderId(context));
        mSeraphixDataProvider.setOnDataUpdated(mDataProviderListener);
        getViewTreeObserver().addOnGlobalLayoutListener(this::onGlobalLayout);
    }

    @Override
    public void onDataUpdated() {
        if (mIsQuickEvent != mController.isQuickEvent()) {
            mIsQuickEvent = mController.isQuickEvent();
            loadViews();
        }
        mWeatherAvailable = mController.isWeatherAvailable() &&
                mController.getEventController().isDeviceIntroCompleted();
        loadWeather();
        loadEvent();
        updateSeparatorVisibility();
    }

    private void loadEvent() {
        mTitle.setEventMode(mIsQuickEvent);
        mEventContainer.setVisibility(mIsQuickEvent ? View.VISIBLE : View.GONE);
        mEventContainer.setOnClickListener(mIsQuickEvent ? null : mController.getEventController().getAction());

        if (!mIsQuickEvent) {
            mTitle.onVisibilityAggregated(true);
            applyAccentTinting();
            return;
        }

        mTitle.setText(mController.getEventController().getTitle());
        mTitle.setOnClickListener(Utilities.showDateInPlaceOfNowPlaying(getContext()) ?
            mActionReceiver.getCalendarAction() : mController.getEventController().getAction());

        mEventContainer.setOnClickListener(mController.getEventController().getAction());
        mEventText.setText(mController.getEventController().getActionTitle());
        mEventText.setMarqueeRepeatLimit(-1);
        mEventText.setSelected(true);
        mEventIcon.setImageResource(mController.getEventController().getActionIcon());
        Utilities.addShadowToImageView(mEventIcon, 5f, 64);
        
        applyAccentTinting();
    }

    private void loadWeather() {
        mWeatherContainer.setVisibility(mWeatherAvailable ? View.VISIBLE : View.GONE);

        if (mWeatherAvailable) {
            mWeatherContainer.setOnClickListener(mActionReceiver.getWeatherAction());
            mWeatherTemp.setText(mController.getWeatherTemp());
            mWeatherIcon.setImageIcon(mController.getWeatherIcon());
            Utilities.addShadowToImageView(mWeatherIcon, 5f, 64);
        }
        
        updateSeparatorVisibility();
        applyAccentTinting();
    }

    private void loadViews() {
        mTitle = (DateTextView) findViewById(R.id.quickspace_title);

        mEventContainer = (ViewGroup) findViewById(R.id.quick_event_container);
        mEventIcon = (ImageView) findViewById(R.id.quick_event_icon);
        mEventText = (TextView) findViewById(R.id.quick_event_text);

        mSeparator = (TextView) findViewById(R.id.quickspace_separator);

        mWeatherContainer = (ViewGroup) findViewById(R.id.quick_event_weather_container);
        mWeatherIcon = (ImageView) findViewById(R.id.quick_event_weather_icon);
        mWeatherTemp = (TextView) findViewById(R.id.quick_event_weather_temp);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mController != null && mFinishedInflate) {
            mController.addListener(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mController != null) {
            mController.removeListener(this);
        }
        if (mSeraphixDataProvider != null) {
            mSeraphixDataProvider.unbind();
        }
    }

    private void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this::onGlobalLayout);
        if (mSeraphixDataProvider == null) {
            return;
        }
        if (isAttachedToWindow()) {
            mSeraphixDataProvider.bind((id) -> Utilities.setSeraphixHolderId(getContext(), id));
        } else {
            mSeraphixDataProvider.unbind();
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        loadViews();
        mFinishedInflate = true;
        if (isAttachedToWindow()) {
            if (mController != null) {
                mController.addListener(this);
            }
        }
    }

    public void onPause() {
        if (mController != null) {
            mController.onPause();
        }
    }

    public void onResume() {
        if (mController != null && mFinishedInflate) {
            mController.addListener(this);
        }
        if (mController != null) {
            mController.onResume();
        }
    }

    public void onDestroy() {
        if (mController != null) {
            mController.onDestroy();
        }
        if (mActionReceiver != null) {
            mActionReceiver = null;
        }
        mController = null;
        if (mSeraphixDataProvider != null) {
            mSeraphixDataProvider.unbind();
            mSeraphixDataProvider = null;
        }
    }

    private final DataProviderListener mDataProviderListener = new DataProviderListener() {
        @Override
        public void onDataUpdated(@NonNull Card card) {
            if (mController == null) return;
            try {
                mController.updateWeatherData(card.getText(), card.getImage());
            } catch (Exception e) {
                Log.e(TAG, "Error updating weather data", e);
            }
        }
    };

    private void updateSeparatorVisibility() {
        if (mSeparator == null) return;
        
        // Show separator only when weather is available AND there's preceding content (event container is visible)
        // Don't show separator if only weather is displayed (when mIsQuickEvent is false)
        boolean showSeparator = mWeatherAvailable && mIsQuickEvent;
        
        mSeparator.setVisibility(showSeparator ? View.VISIBLE : View.GONE);
    }

    private void applyAccentTinting() {
        boolean accentTintEnabled = Utilities.isQuickspaceAccentTintEnabled(getContext());
        
        if (accentTintEnabled) {
            int accentColor = Themes.getColorAccent(getContext());
            
            // Apply accent color to all text elements
            if (mTitle != null) {
                mTitle.setTextColor(accentColor);
            }
            if (mEventText != null) {
                mEventText.setTextColor(accentColor);
            }
            if (mWeatherTemp != null) {
                mWeatherTemp.setTextColor(accentColor);
            }
            if (mSeparator != null) {
                mSeparator.setTextColor(accentColor);
            }
            
            // Apply accent color to all icon elements
            if (mEventIcon != null) {
                mEventIcon.setImageTintList(ColorStateList.valueOf(accentColor));
            }
            if (mWeatherIcon != null) {
                mWeatherIcon.setImageTintList(ColorStateList.valueOf(accentColor));
            }
        } else {
            // Reset text colors to original workspace text color
            int originalTextColor = Themes.getAttrColor(getContext(), R.attr.workspaceTextColor);
            if (mTitle != null) {
                mTitle.setTextColor(originalTextColor);
            }
            if (mEventText != null) {
                mEventText.setTextColor(originalTextColor);
            }
            if (mWeatherTemp != null) {
                mWeatherTemp.setTextColor(originalTextColor);
            }
            if (mSeparator != null) {
                mSeparator.setTextColor(originalTextColor);
            }
            
            // Reset icons to white when accent tint is disabled
            if (mEventIcon != null) {
                mEventIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            }
            if (mWeatherIcon != null) {
                mWeatherIcon.setImageTintList(null); // Remove tint for weather icon to show original colors
            }
        }
    }

}
