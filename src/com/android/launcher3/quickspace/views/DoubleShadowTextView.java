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
package com.android.launcher3.quickspace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.launcher3.views.ShadowInfo;

public class DoubleShadowTextView extends TextView {

    private final ShadowInfo mShadowInfo;

    public DoubleShadowTextView(Context context) {
        this(context, null);
    }

    public DoubleShadowTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DoubleShadowTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mShadowInfo = ShadowInfo.Companion.fromContext(context, attrs, defStyleAttr);
        setShadowLayer(Math.max(mShadowInfo.getKeyShadowBlur() + mShadowInfo.getKeyShadowOffsetY(), mShadowInfo.getAmbientShadowBlur()), 0f, 0f, mShadowInfo.getKeyShadowColor());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (skipDoubleShadow()) {
            super.onDraw(canvas);
            return;
        }
        getPaint().setShadowLayer(mShadowInfo.getAmbientShadowBlur(), 0.0f, 0.0f, mShadowInfo.getAmbientShadowColor());
        super.onDraw(canvas);
        getPaint().setShadowLayer(mShadowInfo.getKeyShadowBlur(), mShadowInfo.getKeyShadowOffsetX(), mShadowInfo.getKeyShadowOffsetY(), mShadowInfo.getKeyShadowColor());
        super.onDraw(canvas);
    }

    private boolean skipDoubleShadow() {
        int textAlpha = android.graphics.Color.alpha(getCurrentTextColor());
        int keyShadowAlpha = android.graphics.Color.alpha(mShadowInfo.getKeyShadowColor());
        int ambientShadowAlpha = android.graphics.Color.alpha(mShadowInfo.getAmbientShadowColor());
        if (textAlpha == 0 || (keyShadowAlpha == 0 && ambientShadowAlpha == 0)) {
            getPaint().clearShadowLayer();
            return true;
        }
        return false;
    }
}
