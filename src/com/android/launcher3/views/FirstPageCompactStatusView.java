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
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Themes;

import java.util.Locale;

/**
 * Compact first-page status strip that mirrors the lockscreen right-side smartspace content.
 */
public class FirstPageCompactStatusView extends FrameLayout {

    private static final String PIXEL_WEATHER_PACKAGE = "com.google.android.apps.weather";

    private final AlarmManager mAlarmManager;
    @Nullable
    private final ApiWrapper.WeatherDataProvider mWeatherDataProvider;
    private final int mWorkspaceTextColor;
    private final int mSecondaryRowPlaceholderHeight;

    private LinearLayout mDateContainer;
    private TextView mDateTextView;
    private LinearLayout mTimerContainer;
    private ImageView mTimerIconView;
    private Chronometer mTimerTextView;
    private LinearLayout mSecondaryRow;
    private LinearLayout mWeatherContainer;
    private ImageView mWeatherIconView;
    private TextView mWeatherTextView;
    private TextView mWeatherForecastTextView;
    private LinearLayout mAlarmContainer;
    private TextView mAlarmTextView;

    private boolean mReceiverRegistered;
    private boolean mForceTwoLineLayout;

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
        mSecondaryRowPlaceholderHeight = getResources().getDimensionPixelSize(
                R.dimen.first_page_status_secondary_row_placeholder_height);
        bindViews();
        refreshDateAndAlarm();
        updateStatusInfo(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerTimeReceiver();
        if (mWeatherDataProvider != null) {
            mWeatherDataProvider.setCallback(this::updateStatusInfo);
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
        mDateContainer = findViewById(R.id.first_page_status_date_container);
        mDateTextView = findViewById(R.id.first_page_status_date);
        mTimerContainer = findViewById(R.id.first_page_status_timer_container);
        mTimerIconView = findViewById(R.id.first_page_status_timer_icon);
        mTimerTextView = findViewById(R.id.first_page_status_timer);
        mSecondaryRow = findViewById(R.id.first_page_status_secondary_row);
        mWeatherContainer = findViewById(R.id.first_page_status_weather_container);
        mWeatherIconView = findViewById(R.id.first_page_status_weather_icon);
        mWeatherTextView = findViewById(R.id.first_page_status_weather);
        mWeatherForecastTextView = findViewById(R.id.first_page_status_weather_forecast);
        mAlarmContainer = findViewById(R.id.first_page_status_alarm_container);
        mAlarmTextView = findViewById(R.id.first_page_status_alarm);
        mDateContainer.setOnClickListener(v -> openCalendar());
        mDateContainer.setClickable(true);
        mDateContainer.setFocusable(true);
        mWeatherContainer.setOnClickListener(v -> openWeather());
        mWeatherContainer.setClickable(true);
        mWeatherContainer.setFocusable(true);
        mSecondaryRow.setMinimumHeight(mSecondaryRowPlaceholderHeight);
    }

    public void setForceTwoLineLayout(boolean forceTwoLineLayout) {
        if (mForceTwoLineLayout == forceTwoLineLayout) {
            return;
        }
        mForceTwoLineLayout = forceTwoLineLayout;
        updateSecondaryRowVisibility();
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

    private void updateStatusInfo(@Nullable ApiWrapper.WeatherInfo weatherInfo) {
        CharSequence weatherText = weatherInfo == null ? null : weatherInfo.getText();
        if (TextUtils.isEmpty(weatherText)) {
            mWeatherContainer.setVisibility(GONE);
            mWeatherTextView.setText(null);
            mWeatherIconView.setImageDrawable(null);
            mWeatherIconView.setImageTintList(null);
            mWeatherForecastTextView.setVisibility(GONE);
            mWeatherForecastTextView.setText(null);
        } else {
            Drawable weatherIcon = weatherInfo.getIcon();
            if (weatherIcon == null) {
                weatherIcon = ContextCompat.getDrawable(
                        getContext(), R.drawable.ic_first_page_weather);
            }
            mWeatherTextView.setText(weatherText);
            mWeatherIconView.setImageDrawable(weatherIcon);
            mWeatherIconView.setImageTintList(
                    weatherInfo.shouldTintIcon()
                            ? ColorStateList.valueOf(mWorkspaceTextColor)
                            : null);
            mWeatherContainer.setVisibility(VISIBLE);

            CharSequence forecastText = weatherInfo.getForecastText();
            if (TextUtils.isEmpty(forecastText)) {
                mWeatherForecastTextView.setVisibility(GONE);
                mWeatherForecastTextView.setText(null);
            } else {
                mWeatherForecastTextView.setText(forecastText);
                mWeatherForecastTextView.setVisibility(VISIBLE);
            }
        }

        CharSequence timerText = weatherInfo == null ? null : weatherInfo.getTimerText();
        if (TextUtils.isEmpty(timerText)) {
            mTimerTextView.stop();
            mTimerContainer.setVisibility(GONE);
            mTimerTextView.setText(null);
            mTimerIconView.setImageDrawable(null);
            mTimerIconView.setImageTintList(null);
        } else {
            Drawable timerIcon = weatherInfo.getTimerIcon();
            if (timerIcon == null) {
                timerIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_first_page_timer);
            }
            mTimerTextView.setText(timerText);
            long timerBase = weatherInfo.getTimerBaseElapsedRealtime();
            if (timerBase > 0L) {
                mTimerTextView.setBase(timerBase);
                mTimerTextView.setCountDown(weatherInfo.shouldCountDownTimer());
                mTimerTextView.start();
            } else {
                mTimerTextView.stop();
            }
            mTimerIconView.setImageDrawable(timerIcon);
            mTimerIconView.setImageTintList(
                    weatherInfo.shouldTintTimerIcon()
                            ? ColorStateList.valueOf(mWorkspaceTextColor)
                            : null);
            mTimerContainer.setVisibility(VISIBLE);
        }

        updateSecondaryRowVisibility();
    }

    private void updateSecondaryRowVisibility() {
        boolean hasSecondaryContent = mWeatherContainer.getVisibility() == VISIBLE
                || mWeatherForecastTextView.getVisibility() == VISIBLE
                || mAlarmContainer.getVisibility() == VISIBLE;
        mSecondaryRow.setVisibility(
                hasSecondaryContent
                        ? VISIBLE
                        : (mForceTwoLineLayout ? INVISIBLE : GONE));
    }

    private void openCalendar() {
        launchIntent(
                mDateContainer,
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR));
    }

    private void openWeather() {
        Intent fallbackIntent = getContext().getPackageManager()
                .getLaunchIntentForPackage(PIXEL_WEATHER_PACKAGE);
        if (fallbackIntent != null) {
            launchIntent(mWeatherContainer, fallbackIntent);
        }
    }

    private void launchIntent(View source, Intent intent) {
        Launcher.getLauncher(getContext()).startActivitySafely(source, intent, null);
    }
}
