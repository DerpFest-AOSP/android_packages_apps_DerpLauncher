/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.qsb;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

public class AiModeButtonView extends ImageView {
    private static final String TAG = "AiModeButtonView";

    public AiModeButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER);
        setOnClickListener(view -> {
            launchAiActivity(context);
        });
    }

    public AiModeButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScaleType(ScaleType.CENTER);
        setOnClickListener(view -> {
            launchAiActivity(context);
        });
    }

    private void launchAiActivity(Context context) {
        // Try multiple AI-related activities in order of preference
        // Based on actual available activities from device dump
        String[] aiActivities = {
            "com.google.android.googlequicksearchbox.OneSearchAimActivity", // Force enabled via component overrides
            "com.google.android.googlequicksearchbox.GeminiGatewayActivity",
            "com.google.android.googlequicksearchbox.SearchActivity",
            "com.google.android.googlequicksearchbox.VoiceSearchActivity",
            "com.google.android.googlequicksearchbox.OneSearchActivity",
            "com.google.android.googlequicksearchbox.GoogleAppVoiceAssistEntrypoint"
        };

        for (String activityName : aiActivities) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName(Utilities.GSA_PACKAGE, activityName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Successfully launched AI activity: " + activityName);
                return; // Success, exit the method
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "Activity not found: " + activityName);
                continue; // Try the next activity
            }
        }

        // Try intent-based approaches for AI functionality
        String[] aiIntents = {
            "com.google.android.PIXEL_SEARCH",
            "android.intent.action.ASSIST",
            "android.intent.action.VOICE_ASSIST",
            "com.samsung.android.intent.action.AI_ASSIST"
        };

        for (String intentAction : aiIntents) {
            try {
                Intent intent = new Intent(intentAction)
                        .setPackage(Utilities.GSA_PACKAGE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Successfully launched AI search via intent action: " + intentAction);
                return;
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "AI intent action not found: " + intentAction);
            }
        }

        // Final fallback: use the original voice command behavior
        try {
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setPackage(QsbContainerView.getSearchWidgetPackageName(context));
            context.startActivity(intent);
            Log.d(TAG, "Falling back to voice command");
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No AI or voice command activities found");
            Toast.makeText(context, R.string.ai_mode_not_available, Toast.LENGTH_SHORT).show();
        }
    }
}
