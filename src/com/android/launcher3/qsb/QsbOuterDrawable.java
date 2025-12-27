/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.qsb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;

public class QsbOuterDrawable extends Drawable {
    private final Paint mPaint;
    private final RectF mRect;
    private final Context mContext;
    private int mOpacity = 70;

    public QsbOuterDrawable(Context context) {
        mContext = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRect = new RectF();
        updateOpacity();
    }

    public void updateOpacity() {
        mOpacity = Utilities.getQsbOuterOpacity(mContext);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        // Get the themed color for outer shape
        int themedColor = Themes.getAttrColor(mContext, R.attr.folderBackgroundColor);

        // Apply opacity to outer shape only
        int alpha = (int) (mOpacity * 255 / 100);
        int outerColorWithAlpha = (themedColor & 0x00FFFFFF) | (alpha << 24);

        mPaint.setColor(outerColorWithAlpha);

        // Draw outer shape
        float radius = mContext.getResources().getDimensionPixelSize(
                R.dimen.qsb_widget_height);
        mRect.set(bounds);
        canvas.drawRoundRect(mRect, radius, radius, mPaint);

        // Match bg_google_qsb_outer insets
        int padding = mContext.getResources().getDimensionPixelSize(R.dimen.qsb_icon_padding);
        int rightPadding = mContext.getResources().getDimensionPixelSize(R.dimen.qsb_inner_inset_end);

        Rect innerBounds = new Rect(
                bounds.left + padding,
                bounds.top + padding,
                bounds.right - rightPadding,
                bounds.bottom - padding
        );

        int fillColor = mContext.getColor(R.color.materialColorSurfaceBright);
        mPaint.setColor(fillColor);

        RectF innerRect = new RectF(innerBounds);
        canvas.drawRoundRect(innerRect, radius, radius, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // Not used
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not used
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
