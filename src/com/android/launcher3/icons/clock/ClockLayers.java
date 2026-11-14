package com.android.launcher3.icons.clock;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;

import java.util.Calendar;
import java.util.TimeZone;

@TargetApi(26)
class ClockLayers {
    AdaptiveIconDrawable mDrawable;
    LayerDrawable mLayerDrawable;
    private final Calendar mCurrentTime;
    int mHourIndex;
    int mMinuteIndex;
    int mSecondIndex;
    int mDefaultHour;
    int mDefaultMinute;
    int mDefaultSecond;
    float scale;
    float offset;
    Bitmap bitmap;

    ClockLayers() {
        mCurrentTime = Calendar.getInstance();
    }

    /**
     * Accepts the two clock shapes icon packs actually ship: an adaptive icon whose foreground
     * is a layer-list, or a layer-list on its own. Anything else is ignored so icon loading
     * can fall back to the static drawable instead of crashing the loader.
     */
    public final void setDrawable(Drawable drawable) {
        mDrawable = null;
        mLayerDrawable = null;
        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) drawable;
            LayerDrawable layers = findLayerDrawable(adaptive.getForeground());
            if (layers != null) {
                mDrawable = adaptive;
                mLayerDrawable = layers;
            }
            return;
        }
        LayerDrawable layers = findLayerDrawable(drawable);
        if (layers != null) {
            mLayerDrawable = layers;
            mDrawable = new AdaptiveIconDrawable(null, layers);
        }
    }

    private static LayerDrawable findLayerDrawable(Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            return (LayerDrawable) drawable;
        }
        if (drawable instanceof InsetDrawable) {
            Drawable inner = ((InsetDrawable) drawable).getDrawable();
            if (inner instanceof LayerDrawable) {
                return (LayerDrawable) inner;
            }
        }
        return null;
    }

    @Override
    public ClockLayers clone() {
        if (mDrawable == null || mDrawable.getConstantState() == null) {
            return null;
        }
        ClockLayers clone = new ClockLayers();
        clone.scale = scale;
        clone.offset = offset;
        clone.mHourIndex = mHourIndex;
        clone.mMinuteIndex = mMinuteIndex;
        clone.mSecondIndex = mSecondIndex;
        clone.mDefaultHour = mDefaultHour;
        clone.mDefaultMinute = mDefaultMinute;
        clone.mDefaultSecond = mDefaultSecond;
        clone.setDrawable(mDrawable.getConstantState().newDrawable());
        clone.bitmap = bitmap;
        if (clone.mLayerDrawable == null) {
            return null;
        }
        return clone;
    }

    boolean updateAngles() {
        if (mLayerDrawable == null) {
            return false;
        }
        mCurrentTime.setTimeInMillis(System.currentTimeMillis());

        int hour = (mCurrentTime.get(Calendar.HOUR) + (12 - mDefaultHour)) % 12;
        int minute = (mCurrentTime.get(Calendar.MINUTE) + (60 - mDefaultMinute)) % 60;
        int second = (mCurrentTime.get(Calendar.SECOND) + (60 - mDefaultSecond)) % 60;

        boolean hasChanged = false;
        Drawable hourDrawable = mHourIndex != -1 ? mLayerDrawable.getDrawable(mHourIndex) : null;
        if (hourDrawable != null && hourDrawable.setLevel(
                hour * 60 + mCurrentTime.get(Calendar.MINUTE))) {
            hasChanged = true;
        }
        Drawable minuteDrawable = mMinuteIndex != -1 ? mLayerDrawable.getDrawable(mMinuteIndex) : null;
        if (minuteDrawable != null && minuteDrawable.setLevel(
                minute + mCurrentTime.get(Calendar.HOUR) * 60)) {
            hasChanged = true;
        }
        Drawable secondDrawable = mSecondIndex != -1 ? mLayerDrawable.getDrawable(mSecondIndex) : null;
        if (secondDrawable != null && secondDrawable.setLevel(second * 10)) {
            hasChanged = true;
        }
        return hasChanged;
    }

    void setTimeZone(TimeZone timeZone) {
        mCurrentTime.setTimeZone(timeZone);
    }
}
