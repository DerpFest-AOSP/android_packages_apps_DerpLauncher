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
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.launcher3.Launcher;

/**
 * Launches voice search from the hotseat QSB.
 */
public class AssistantIconView extends ImageView {

    public AssistantIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistantIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.CENTER);
        setOnClickListener(v -> launchVoiceSearch(context));
    }

    private void launchVoiceSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String searchPackage = QsbLayout.getSearchPackage(context);
        if (searchPackage != null) {
            intent.setPackage(searchPackage);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException ignored) {
            Launcher.getLauncher(context).startSearch("", false, null, true);
        }
    }
}
