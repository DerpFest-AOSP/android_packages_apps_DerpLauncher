package com.android.launcher3.icons;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;

import android.annotation.SuppressLint;
import com.android.launcher3.util.ComponentKey;

import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.dagger.LauncherAppSingleton;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.icons.pack.IconResolver;
import com.android.launcher3.util.ApiWrapper;

import javax.inject.Inject;

import static com.android.launcher3.icons.BaseIconFactory.CONFIG_HINT_NO_WRAP;

@LauncherAppSingleton
public class ThirdPartyIconProvider extends LauncherIconProvider {

    private final Context mContext;
    private final ThemeManager mThemeManager;
    private final ApiWrapper mApiWrapper;

    @Inject
    public ThirdPartyIconProvider(
            @ApplicationContext Context context,
            ThemeManager themeManager,
            ApiWrapper apiWrapper) {
        super(context, themeManager, apiWrapper);
        mContext = context;
        mThemeManager = themeManager;
        mApiWrapper = apiWrapper;
    }

    public ThirdPartyIconProvider(Context context) {
        this(context, ThemeManager.INSTANCE.get(context), ApiWrapper.INSTANCE.get(context));
    }

    @SuppressLint("WrongConstant")
    @Override
    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int iconDpi) {
        ComponentKey key = new ComponentKey(
                launcherActivityInfo.getComponentName(), launcherActivityInfo.getUser());

        IconResolver.DefaultDrawableProvider fallback =
                () -> super.getIcon(launcherActivityInfo, iconDpi);
        Drawable icon = ThirdPartyIconUtils.getByKey(mContext, key, iconDpi, fallback);

        if (icon == null) {
            return fallback.get();
        }
        icon.setChangingConfigurations(icon.getChangingConfigurations() | CONFIG_HINT_NO_WRAP);
        return icon;
    }
}
