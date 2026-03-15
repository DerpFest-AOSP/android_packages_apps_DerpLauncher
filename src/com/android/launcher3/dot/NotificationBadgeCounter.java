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
package com.android.launcher3.dot;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

import androidx.core.graphics.ColorUtils;

import com.android.launcher3.icons.DotRenderer;

/**
 * Draws notification counts using the same anchor and color as notification dots.
 *
 * Adapted from Lunaris NotificationBadgeCounter to A17 DotRenderer (RadialGradient shadow,
 * no ShadowGenerator.Builder).
 */
public class NotificationBadgeCounter {

    private static final float SIZE_PERCENTAGE = 0.26f;
    private static final float TEXT_SIZE_PERCENTAGE = 0.70f;
    private static final float HORIZONTAL_PADDING_PERCENTAGE = 0.32f;
    private static final double MIN_TEXT_CONTRAST = 4.5;
    private static final int MAX_DISPLAY_COUNT = 99;
    private static final float SHADOW_FACTOR = 1f / 12;

    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBadgeBounds = new RectF();
    private final RectF mShadowBounds = new RectF();
    private float mCachedShadowRadius = -1;

    public void draw(Canvas canvas, DotRenderer.DrawParams params, int dotColor, int count) {
        if (params == null || count <= 0 || params.scale <= 0) {
            return;
        }

        String countText = count > MAX_DISPLAY_COUNT
                ? MAX_DISPLAY_COUNT + "+"
                : String.valueOf(count);
        Rect iconBounds = params.iconBounds;
        PointF dotPosition = params.getDotPosition();
        float dotCenterX = iconBounds.left + iconBounds.width() * dotPosition.x;
        float dotCenterY = iconBounds.top + iconBounds.height() * dotPosition.y;

        int badgeHeight = Math.max(1, Math.round(SIZE_PERCENTAGE * iconBounds.width()));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
        mTextPaint.setTextSize(badgeHeight * TEXT_SIZE_PERCENTAGE);
        mTextPaint.setColor(getTextColor(dotColor));

        int badgeWidth = Math.max(badgeHeight, Math.round(mTextPaint.measureText(countText)
                + badgeHeight * HORIZONTAL_PADDING_PERCENTAGE * 2));
        float shadowSize = badgeHeight * SHADOW_FACTOR;
        float drawnHalfWidth = badgeWidth / 2f + shadowSize;
        float drawnHalfHeight = badgeHeight / 2f + shadowSize;

        Rect canvasBounds = canvas.getClipBounds();
        float offsetX = params.leftAlign
                ? Math.max(0, canvasBounds.left - (dotCenterX - drawnHalfWidth))
                : Math.min(0, canvasBounds.right - (dotCenterX + drawnHalfWidth));
        float offsetY = Math.max(0, canvasBounds.top - (dotCenterY - drawnHalfHeight));

        canvas.save();
        canvas.translate(dotCenterX + offsetX, dotCenterY + offsetY);
        canvas.scale(params.scale, params.scale);

        mBadgeBounds.set(-badgeWidth / 2f, -badgeHeight / 2f, badgeWidth / 2f, badgeHeight / 2f);
        mShadowBounds.set(mBadgeBounds);
        mShadowBounds.inset(-shadowSize, -shadowSize);

        float shadowRadius = Math.max(drawnHalfWidth, drawnHalfHeight);
        if (mCachedShadowRadius != shadowRadius) {
            mShadowPaint.setShader(new RadialGradient(0, 0, shadowRadius,
                    new int[] { Color.BLACK, Color.TRANSPARENT },
                    new float[] { 0.3f, 1f }, TileMode.CLAMP));
            mCachedShadowRadius = shadowRadius;
        }
        canvas.drawRoundRect(mShadowBounds, drawnHalfHeight, drawnHalfHeight, mShadowPaint);

        mBackgroundPaint.setColor(dotColor);
        canvas.drawRoundRect(mBadgeBounds, badgeHeight / 2f, badgeHeight / 2f, mBackgroundPaint);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float textBaseline = -(fontMetrics.ascent + fontMetrics.descent) / 2;
        canvas.drawText(countText, 0, textBaseline, mTextPaint);
        canvas.restore();
    }

    private static int getTextColor(int backgroundColor) {
        return ColorUtils.calculateContrast(Color.WHITE, backgroundColor) >= MIN_TEXT_CONTRAST
                ? Color.WHITE
                : Color.BLACK;
    }
}
