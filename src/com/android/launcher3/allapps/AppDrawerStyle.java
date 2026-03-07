/*
 * SPDX-FileCopyrightText: VoltageOS
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.allapps;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;

/**
 * App drawer presentation styles.
 * <ul>
 *   <li>{@link #NORMAL}: stock bottom sheet with a vertically scrolling A-Z grid.</li>
 *   <li>{@link #HORIZONTAL_LIST}: bottom sheet with one app per row (icon + label side by side).</li>
 *   <li>{@link #VERTICAL_PAGED}: fullscreen, horizontally paged grid (One UI style).</li>
 *   <li>{@link #FULLSCREEN}: fullscreen panel with a vertically scrolling A-Z grid.</li>
 * </ul>
 */
public final class AppDrawerStyle {

    public static final String NORMAL = "normal";
    public static final String HORIZONTAL_LIST = "horizontal_list";
    public static final String VERTICAL_PAGED = "vertical";
    public static final String FULLSCREEN = "fullscreen";

    private AppDrawerStyle() { }

    /** Returns the user-selected style, falling back to {@link #NORMAL} for unknown values. */
    public static String get(Context context) {
        String style = LauncherPrefs.APP_DRAWER_STYLE.get(context);
        return isSupported(style) ? style : NORMAL;
    }

    public static boolean isSupported(@Nullable String style) {
        return NORMAL.equals(style)
                || HORIZONTAL_LIST.equals(style)
                || VERTICAL_PAGED.equals(style)
                || FULLSCREEN.equals(style);
    }

    public static boolean isNormal(@Nullable String style) {
        return style == null || NORMAL.equals(style);
    }

    public static boolean isHorizontalList(@Nullable String style) {
        return HORIZONTAL_LIST.equals(style);
    }

    public static boolean isVerticalPaged(@Nullable String style) {
        return VERTICAL_PAGED.equals(style);
    }

    /** Fullscreen styles have no rounded sheet; the paged style is always fullscreen. */
    public static boolean isFullscreen(@Nullable String style) {
        return FULLSCREEN.equals(style) || VERTICAL_PAGED.equals(style);
    }

    public static boolean isFullscreen(Context context) {
        return isFullscreen(get(context));
    }
}
