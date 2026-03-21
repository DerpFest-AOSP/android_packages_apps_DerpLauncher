/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.allapps.search;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.launcher3.Utilities.prefixTextWithIcon;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.PopupMenu;

import androidx.core.graphics.ColorUtils;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.allapps.PrivateProfileManager;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Themes;
import com.android.launcher3.graphics.ThemeManager;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;

/**
 * Layout to contain the All-apps search UI.
 */
public class AppsSearchContainerLayout extends ExtendedEditText
        implements SearchUiManager, SearchCallback<AdapterItem>,
        AllAppsStore.OnUpdateListener, Insettable {

    private final ActivityContext mLauncher;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    private ActivityAllAppsContainerView<?> mAppsView;

    // The amount of pixels to shift down and overlap with the rest of the content.
    private final int mContentOverlap;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = ActivityContext.lookupContext(context);
        mSearchBarController = new AllAppsSearchBarController();

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);

        mContentOverlap =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_content_overlap);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applySearchBarVisualStyle();
        if (mAppsView != null) {
            mAppsView.getAppsStore().addUpdateListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mAppsView != null)
            mAppsView.getAppsStore().removeUpdateListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Update the width to match the grid padding
        if (mAppsView == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int myRequestedWidth = getSize(widthMeasureSpec);
        View widthSource = mAppsView.getActiveRecyclerView();
        if (widthSource == null) {
            widthSource = mAppsView.getAppsRecyclerViewContainer();
        }
        if (widthSource == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int rowWidth = myRequestedWidth - widthSource.getPaddingLeft()
                - widthSource.getPaddingRight();

        int cellWidth = DeviceProfile.calculateCellWidth(rowWidth,
                dp.getWorkspaceIconProfile().getCellLayoutBorderSpacePx().x, dp.numShownHotseatIcons);
        int iconVisibleSize =
                Math.round(ICON_VISIBLE_AREA_FACTOR * dp.getWorkspaceIconProfile().getIconSizePx());
        int iconPadding = cellWidth - iconVisibleSize;

        int myWidth = rowWidth - iconPadding + getPaddingLeft() + getPaddingRight();
        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Drawable gIcon = getContext().getDrawable(R.drawable.ic_allapps_g_color);
        Drawable gIconThemed = getContext().getDrawable(R.drawable.ic_allapps_g_themed);
        Drawable sIcon = getContext().getDrawable(R.drawable.ic_allapps_search);
        Drawable actions = getContext().getDrawable(R.drawable.ic_allapps_actions_color);
        Drawable actionsThemed = getContext().getDrawable(R.drawable.ic_allapps_actions_themed);
        Drawable optionsIcon = getContext().getDrawable(R.drawable.ic_more_vert_dots);
        if (optionsIcon != null) {
            optionsIcon.setTint(Themes.getAttrColor(getContext(), android.R.attr.textColorPrimary));
        }

        // Shift the widget horizontally so that its centered in the parent (b/63428078)
        View parent = (View) getParent();
        if (parent != null) {
            int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            int myWidth = right - left;
            int expectedLeft = parent.getPaddingLeft() + (availableWidth - myWidth) / 2;
            int shift = expectedLeft - left;
            setTranslationX(shift);
        }

        boolean showQSB = Utilities.showQSB(getContext());
        boolean isDockThemed = ThemeManager.INSTANCE.get(getContext()).isMonoThemeEnabled();
        boolean hasGoogleApp = Utilities.isGSAEnabled(getContext());

        Drawable leftStart;
        Drawable endDrawable;
        if (showQSB) {
            leftStart = isDockThemed ? gIconThemed : gIcon;
            endDrawable = mergeActionsAndSortDrawable(
                    isDockThemed ? actionsThemed : actions, optionsIcon);
        } else {
            leftStart = sIcon;
            endDrawable = mergeActionsAndSortDrawable(actions, optionsIcon);
        }
        if (leftStart != null) {
            int lw = leftStart.getIntrinsicWidth();
            int lh = leftStart.getIntrinsicHeight();
            if (lw <= 0) {
                lw = (int) (24 * getResources().getDisplayMetrics().density);
            }
            if (lh <= 0) {
                lh = (int) (24 * getResources().getDisplayMetrics().density);
            }
            leftStart.setBounds(0, 0, lw, lh);
        }
        if (endDrawable != null) {
            Rect eb = endDrawable.getBounds();
            endDrawable.setBounds(0, 0, eb.width(), eb.height());
        }
        setCompoundDrawablesRelative(leftStart, null, endDrawable, null);

        int leftSlotWidth = getResources().getDimensionPixelSize(R.dimen.qsb_icon_tap_size);
        int actionSlotWidth = getResources().getDimensionPixelSize(R.dimen.qsb_icon_tap_size);
        int rightGroupWidth = actionSlotWidth * 3;
        int rightGroupStart = getWidth() - getPaddingEnd() - rightGroupWidth;

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float touchX = event.getX();
                    Drawable rightCompound = getCompoundDrawablesRelative()[2];
                    Drawable leftDrawable = getCompoundDrawablesRelative()[0];

                    // Left slot (G icon when showQSB)
                    if (leftDrawable != null && touchX <= (getPaddingStart() + leftSlotWidth)) {
                        if (hasGoogleApp && showQSB) {
                            Intent gIntent = getContext().getPackageManager()
                                    .getLaunchIntentForPackage(Utilities.GSA_PACKAGE);
                            if (gIntent != null) {
                                gIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                getContext().startActivity(gIntent);
                                return true;
                            }
                        }
                        return false;
                    }

                    // Right: mic | lens | sort menu
                    if (rightCompound != null && touchX >= rightGroupStart) {
                        if (touchX < (rightGroupStart + actionSlotWidth)) {
                            String searchPackage = QsbContainerView.getSearchWidgetPackageName(getContext());
                            if (searchPackage != null) {
                                Intent voiceIntent = new Intent(Intent.ACTION_VOICE_COMMAND)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        .setPackage(searchPackage);
                                getContext().startActivity(voiceIntent);
                            }
                        } else if (touchX < (rightGroupStart + 2 * actionSlotWidth)) {
                            if (hasGoogleApp) {
                                Intent lensIntent = new Intent();
                                lensIntent.setAction(Intent.ACTION_VIEW)
                                        .setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .setData(Uri.parse(Utilities.LENS_URI))
                                        .putExtra("LensHomescreenShortcut", true);
                                getContext().startActivity(lensIntent);
                            }
                        } else {
                            showSortingOptions();
                        }
                        return true;
                    }

                    // Middle area – Pixel Search if installed
                    if (touchX > (getPaddingStart() + leftSlotWidth) && touchX < rightGroupStart) {
                        Intent pixelSearchIntent = getContext().getPackageManager()
                                .getLaunchIntentForPackage("rk.android.app.pixelsearch");
                        if (pixelSearchIntent != null) {
                            pixelSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getContext().startActivity(pixelSearchIntent);
                            return true;
                        }
                        return false;
                    }
                }
                return false;
            }
        });

        offsetTopAndBottom(mContentOverlap);
    }

    private void applySearchBarVisualStyle() {
        Context ctx = getContext();
        int color = Themes.getAttrColor(ctx, R.attr.qsbFillColor);
        if (ThemeManager.INSTANCE.get(ctx).isMonoThemeEnabled()) {
            color = Themes.getAttrColor(ctx, R.attr.qsbFillColorThemed);
        }
        color = ColorUtils.setAlphaComponent(color, 80);
        float cornerRadius = getResources().getDimension(R.dimen.rounded_button_radius);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(cornerRadius);
        setBackground(gd);
        setTextColor(Themes.getAttrColor(ctx, android.R.attr.textColorPrimary));
        setHintTextColor(Themes.getAttrColor(ctx, android.R.attr.textColorSecondary));
    }

    private Drawable mergeActionsAndSortDrawable(Drawable actionsDr, Drawable sortDr) {
        if (sortDr == null) {
            return actionsDr;
        }
        Drawable sort = sortDr.mutate();
        sort.setTint(Themes.getAttrColor(getContext(), android.R.attr.textColorPrimary));
        if (actionsDr == null) {
            return sort;
        }
        Drawable actions = actionsDr.mutate();
        float density = getResources().getDisplayMetrics().density;
        int gap = (int) (4 * density);
        int aw = actions.getIntrinsicWidth() > 0 ? actions.getIntrinsicWidth() : (int) (48 * density);
        int sw = sort.getIntrinsicWidth() > 0 ? sort.getIntrinsicWidth() : (int) (24 * density);
        int ah = actions.getIntrinsicHeight() > 0 ? actions.getIntrinsicHeight() : (int) (48 * density);
        int sh = sort.getIntrinsicHeight() > 0 ? sort.getIntrinsicHeight() : (int) (24 * density);
        int height = Math.max(ah, sh);
        int width = aw + gap + sw;
        LayerDrawable ld = new LayerDrawable(new Drawable[]{actions, sort});
        ld.setLayerInset(0, 0, 0, width - aw, 0);
        ld.setLayerInset(1, aw + gap, 0, 0, 0);
        ld.setBounds(0, 0, width, height);
        return ld;
    }

    private void showSortingOptions() {
        PopupMenu popup = new PopupMenu(getContext(), this, Gravity.END);
        popup.getMenu().add(0, 0, 0, getContext().getString(R.string.app_drawer_sort_alphabetical));
        popup.getMenu().add(0, 1, 1, getContext().getString(R.string.app_drawer_sort_install_date));
        popup.getMenu().add(0, 2, 2, getContext().getString(R.string.app_drawer_sort_usage));

        popup.setOnMenuItemClickListener((MenuItem item) -> {
            int sortMode = item.getItemId();
            if (sortMode == 2) {
                AppOpsManager appOps =
                        (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
                if (appOps != null) {
                    int mode = appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            Process.myUid(),
                            getContext().getPackageName());
                    if (mode == AppOpsManager.MODE_DEFAULT) {
                        mode = getContext().checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ? AppOpsManager.MODE_ALLOWED
                                : AppOpsManager.MODE_IGNORED;
                    }
                    if (mode != AppOpsManager.MODE_ALLOWED) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);
                        return true;
                    }
                }
            }
            LauncherPrefs.INSTANCE.get(getContext()).put(LauncherPrefs.APP_DRAWER_SORT_MODE, sortMode);
            if (mAppsView != null) {
                mAppsView.getAppsStore().notifyUpdate();
            }
            return true;
        });
        popup.show();
    }

    @Override
    public void initializeSearch(ActivityAllAppsContainerView<?> appsView) {
        mAppsView = appsView;
        mSearchBarController.initialize(
                new DefaultAppSearchAlgorithm(getContext(), true),
                this, mLauncher, this);
    }

    @Override
    public void onAppsUpdated() {
        mSearchBarController.refreshSearchResult();
    }

    @Override
    public void resetSearch() {
        mSearchBarController.reset();
    }

    @Override
    public boolean focusSearchField() {
        return mSearchBarController.focusSearchField();
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<AdapterItem> items) {
        if (query.equalsIgnoreCase(mContext.getString(R.string.private_space_label))) {
            privateSpaceQuery();
            return;
        }
        if (items != null) {
            mAppsView.setSearchResults(items);
        }
    }

    @Override
    public void clearSearchResult() {
        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
        mAppsView.onClearSearchResult();
        
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        mlp.topMargin = getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_margin_top);
        requestLayout();
    }

    @Override
    public ExtendedEditText getEditText() {
        return this;
    }

    private void privateSpaceQuery() {
        PrivateProfileManager privateProfileManager = mAppsView.getPrivateProfileManager();
        if (privateProfileManager.isPrivateSpaceHidden()) {
            privateProfileManager.setQuietMode(false);
        } else if (!mAppsView.hasPrivateProfile()) {
            final Intent privateSpaceSettingsIntent =
                    ApiWrapper.INSTANCE.get(mContext).getPrivateSpaceSettingsIntent();
            if (privateSpaceSettingsIntent != null) {
                mLauncher.startActivitySafely(mAppsView, privateSpaceSettingsIntent, null);
            }
        }
    }
}
