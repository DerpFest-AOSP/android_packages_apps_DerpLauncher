/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.qsb;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;

/**
 * Launches assistant-like entry points from the hotseat QSB.
 */
public class AiModeButtonView extends ImageView {

    private static final String ACTION_VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";

    public AiModeButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AiModeButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.CENTER);
        setOnClickListener(v -> launchAiEntry(context));
    }

    private void launchAiEntry(Context context) {
        if (Utilities.isAiMusicSearchEnabled(context)) {
            launchMusicSearch(context);
            return;
        }

        String searchPackage = QsbLayout.getSearchPackage(context);
        Intent[] candidates = new Intent[] {
                new Intent("com.google.android.PIXEL_SEARCH")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                new Intent(Intent.ACTION_ASSIST)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                new Intent(ACTION_VOICE_ASSIST)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                new Intent(Intent.ACTION_WEB_SEARCH)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        };
        for (Intent candidate : candidates) {
            if (searchPackage != null) {
                candidate.setPackage(searchPackage);
            }
            if (candidate.resolveActivity(context.getPackageManager()) == null) {
                continue;
            }
            try {
                context.startActivity(candidate);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Try the next entry point.
            }
        }
        Launcher.getLauncher(context).startSearch("", false, null, true);
    }

    private void launchMusicSearch(Context context) {
        String searchPackage = QsbLayout.getSearchPackage(context);
        Intent[] candidates = new Intent[] {
                new Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                new Intent(Intent.ACTION_VOICE_COMMAND)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
        };
        for (Intent candidate : candidates) {
            if (searchPackage != null) {
                candidate.setPackage(searchPackage);
            }
            if (candidate.resolveActivity(context.getPackageManager()) == null) {
                continue;
            }
            try {
                context.startActivity(candidate);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Try the next entry point.
            }
        }
        Launcher.getLauncher(context).startSearch("", false, null, true);
    }
}
