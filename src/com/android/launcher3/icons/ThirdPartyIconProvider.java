package com.android.launcher3.icons;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageItemInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.android.launcher3.icons.pack.IconResolver;
import com.android.launcher3.util.ComponentKey;

public class ThirdPartyIconProvider extends IconProvider {

    public ThirdPartyIconProvider(Context context) {
        super(context);
    }

    @Override
    public Drawable getIcon(PackageItemInfo info, ApplicationInfo appInfo, int iconDpi) {
        if (info instanceof ComponentInfo ci && ci.name != null) {
            ComponentKey key = new ComponentKey(
                    ci.getComponentName(),
                    UserHandle.getUserHandleForUid(appInfo.uid));
            IconResolver.DefaultDrawableProvider fallback =
                    () -> super.getIcon(info, appInfo, iconDpi);
            Drawable icon = ThirdPartyIconUtils.getByKey(mContext, key, iconDpi, fallback);
            if (icon != null) {
                Drawable wrapped = ExtendedBitmapDrawable.wrap(
                        mContext.getResources(), icon, true);
                return wrapped != null ? wrapped : icon;
            }
        }
        return super.getIcon(info, appInfo, iconDpi);
    }
}
