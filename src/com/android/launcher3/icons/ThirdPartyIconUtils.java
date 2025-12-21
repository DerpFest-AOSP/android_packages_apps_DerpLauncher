package com.android.launcher3.icons;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.launcher3.icons.clock.CustomClock;
import com.android.launcher3.icons.pack.IconPackManager;
import com.android.launcher3.icons.pack.IconResolver;
import com.android.launcher3.util.ComponentKey;

class ThirdPartyIconUtils {
    static Drawable getByKey(Context context, ComponentKey key, int iconDpi,
                             IconResolver.DefaultDrawableProvider fallback) {
        IconResolver resolver = IconPackManager.get(context).resolve(key);
        if (resolver == null) {
            return null;
        }
        Drawable icon = resolver.getIcon(iconDpi, fallback);
        if (icon != null && resolver.isClock()) {
            Drawable clock = CustomClock.getClock(context, icon, resolver.clockData());
            return clock != null ? clock : icon;
        }
        return icon;
    }
}
