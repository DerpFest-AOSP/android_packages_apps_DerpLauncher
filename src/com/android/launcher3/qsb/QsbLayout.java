package com.android.launcher3.qsb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Reorderable;
import com.android.launcher3.Utilities;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.util.MultiTranslateDelegate;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.graphics.ThemeManager.ThemeChangeListener;

public class QsbLayout extends FrameLayout implements Reorderable, SharedPreferences.OnSharedPreferenceChangeListener {

    ImageView mAssistantIcon;
    ImageView mGoogleIcon;
    ImageView mLensIcon;
    ImageView mAiModeButton;
    Context mContext;

    private final MultiTranslateDelegate mTranslateDelegate = new MultiTranslateDelegate(this);
    private float mScaleForReorderBounce = 1f;
    private ThemeChangeListener mThemeChangeListener;

    public QsbLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAssistantIcon = findViewById(R.id.mic_icon);
        mGoogleIcon = findViewById(R.id.g_icon);
        mLensIcon = findViewById(R.id.lens_icon);
        mAiModeButton = findViewById(R.id.ai_mode_button);
        setIcons();

        mThemeChangeListener = () -> setIcons();
        ThemeManager.INSTANCE.get(mContext).addChangeListener(mThemeChangeListener);
        LauncherPrefs.getPrefs(mContext).registerOnSharedPreferenceChangeListener(this);

        String searchPackage = QsbContainerView.getSearchWidgetPackageName(mContext);
        setOnClickListener(view -> {
            mContext.startActivity(new Intent("android.search.action.GLOBAL_SEARCH").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK).setPackage(searchPackage));
        });

        if (Utilities.isGSAEnabled(mContext)) {
            enableLensIcon();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mThemeChangeListener != null) {
            ThemeManager.INSTANCE.get(mContext).removeChangeListener(mThemeChangeListener);
            mThemeChangeListener = null;
        }
        LauncherPrefs.getPrefs(mContext).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(LauncherPrefs.DOCK_AI_MUSIC_SEARCH.getSharedPrefKey())) {
            setIcons();
        }
    }

    private void setIcons() {
        boolean isThemed = ThemeManager.INSTANCE.get(mContext).isMonoThemeEnabled();
        boolean isMusicSearch = Utilities.isAiMusicSearchEnabled(mContext);

        if (isThemed) {
            mAssistantIcon.setImageResource(R.drawable.ic_mic_themed);
            mGoogleIcon.setImageResource(R.drawable.ic_super_g_themed);
            mLensIcon.setImageResource(R.drawable.ic_lens_themed);
            mAiModeButton.setImageResource(isMusicSearch ? R.drawable.ic_music_themed : R.drawable.ic_ai_mode_themed);
        } else {
            mAssistantIcon.setImageResource(R.drawable.ic_mic_color);
            mGoogleIcon.setImageResource(R.drawable.ic_super_g_color);
            mLensIcon.setImageResource(R.drawable.ic_lens_color);
            mAiModeButton.setImageResource(isMusicSearch ? R.drawable.ic_music_color : R.drawable.ic_ai_mode_color);
        }
    }

    private void enableLensIcon() {
        mLensIcon.setVisibility(View.VISIBLE);
        mLensIcon.setOnClickListener(view -> {
            Intent lensIntent = new Intent();
            lensIntent.setAction(Intent.ACTION_VIEW)
                    .setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.parse(Utilities.LENS_URI))
                    .putExtra("LensHomescreenShortcut", true);
            mContext.startActivity(lensIntent);
        });
    }

    @Override
    public MultiTranslateDelegate getTranslateDelegate() {
        return mTranslateDelegate;
    }

    @Override
    public void setReorderBounceScale(float scale) {
        mScaleForReorderBounce = scale;
        super.setScaleX(scale);
        super.setScaleY(scale);
    }

    @Override
    public float getReorderBounceScale() {
        return mScaleForReorderBounce;
    }

}
