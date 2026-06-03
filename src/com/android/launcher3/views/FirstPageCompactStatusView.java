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

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.R;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Themes;

import java.util.Locale;

/**
 * Compact first-page status strip that mirrors the lockscreen right-side smartspace content.
 */
public class FirstPageCompactStatusView extends FrameLayout {

    private final AlarmManager mAlarmManager;
    @Nullable
    private final ApiWrapper.WeatherDataProvider mWeatherDataProvider;
    private final int mWorkspaceTextColor;

    private TextView mDateTextView;
    private LinearLayout mSecondaryRow;
    private LinearLayout mWeatherContainer;
    private ImageView mWeatherIconView;
    private TextView mWeatherTextView;
    private LinearLayout mAlarmContainer;
    private TextView mAlarmTextView;

    private boolean mReceiverRegistered;

    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDateAndAlarm();
        }
    };

    public FirstPageCompactStatusView(Context context) {
        this(context, null);
    }

    public FirstPageCompactStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FirstPageCompactStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.first_page_status_content, this, true);
        mAlarmManager = context.getSystemService(AlarmManager.class);
        mWeatherDataProvider = ApiWrapper.INSTANCE.get(context).createWeatherDataProvider();
        mWorkspaceTextColor = Themes.getAttrColor(context, R.attr.workspaceTextColor);
        bindViews();
        refreshDateAndAlarm();
        updateWeather(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerTimeReceiver();
        if (mWeatherDataProvider != null) {
            mWeatherDataProvider.setCallback(this::updateWeather);
            mWeatherDataProvider.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mWeatherDataProvider != null) {
            mWeatherDataProvider.setCallback(null);
            mWeatherDataProvider.stop();
        }
        unregisterTimeReceiver();
    }

    private void bindViews() {
        mDateTextView = findViewById(R.id.first_page_status_date);
        mSecondaryRow = findViewById(R.id.first_page_status_secondary_row);
        mWeatherContainer = findViewById(R.id.first_page_status_weather_container);
        mWeatherIconView = findViewById(R.id.first_page_status_weather_icon);
        mWeatherTextView = findViewById(R.id.first_page_status_weather);
        mAlarmContainer = findViewById(R.id.first_page_status_alarm_container);
        mAlarmTextView = findViewById(R.id.first_page_status_alarm);
    }

    private void registerTimeReceiver() {
        if (mReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        ContextCompat.registerReceiver(
                getContext(), mTimeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;
    }

    private void unregisterTimeReceiver() {
        if (!mReceiverRegistered) {
            return;
        }
        getContext().unregisterReceiver(mTimeReceiver);
        mReceiverRegistered = false;
    }

    private void refreshDateAndAlarm() {
        updateDate();
        updateAlarm();
    }

    private void updateDate() {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE, MMM d");
        mDateTextView.setText(DateFormat.format(pattern, System.currentTimeMillis()));
    }

    private void updateAlarm() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager == null ? null : mAlarmManager.getNextAlarmClock();
        if (nextAlarm == null) {
            mAlarmContainer.setVisibility(GONE);
            updateSecondaryRowVisibility();
            return;
        }

        long triggerTime = nextAlarm.getTriggerTime();
        int formatFlags = DateUtils.FORMAT_SHOW_TIME;
        if (!DateUtils.isToday(triggerTime)) {
            formatFlags |= DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;
        }
        mAlarmTextView.setText(DateUtils.formatDateTime(getContext(), triggerTime, formatFlags));
        mAlarmContainer.setVisibility(VISIBLE);
        updateSecondaryRowVisibility();
    }

    private void updateWeather(@Nullable ApiWrapper.WeatherInfo weatherInfo) {
        if (weatherInfo == null || TextUtils.isEmpty(weatherInfo.getText())) {
            mWeatherContainer.setVisibility(GONE);
            mWeatherTextView.setText(null);
            mWeatherIconView.setImageDrawable(null);
            updateSecondaryRowVisibility();
            return;
        }

        Drawable weatherIcon = weatherInfo.getIcon();
        if (weatherIcon == null) {
            weatherIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_first_page_weather);
        }
        mWeatherTextView.setText(weatherInfo.getText());
        mWeatherIconView.setImageDrawable(weatherIcon);
        mWeatherIconView.setImageTintList(
                weatherInfo.shouldTintIcon()
                        ? ColorStateList.valueOf(mWorkspaceTextColor)
                        : null);
        mWeatherContainer.setVisibility(VISIBLE);
        updateSecondaryRowVisibility();
    }

    private void updateSecondaryRowVisibility() {
        mSecondaryRow.setVisibility(
                mWeatherContainer.getVisibility() == VISIBLE
                                || mAlarmContainer.getVisibility() == VISIBLE
                        ? VISIBLE
                        : GONE);
    }
}
