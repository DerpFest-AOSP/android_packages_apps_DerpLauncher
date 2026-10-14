/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.List;
import java.util.Objects;

/**
 * Provides views for local search results.
 */
public class DefaultSearchAdapterProvider extends SearchAdapterProvider<ActivityContext> {
    private View mHighlightedView;

    public DefaultSearchAdapterProvider(ActivityContext launcher) {
        super(launcher);
    }

    @Override
    public void onBindView(AllAppsGridAdapter.ViewHolder holder, int position) {
        if (position == 0) {
            mHighlightedView = holder.itemView;
        }
    }

    @Override
    public boolean isViewSupported(int viewType) {
        return false;
    }

    @Override
    public AllAppsGridAdapter.ViewHolder onCreateViewHolder(LayoutInflater layoutInflater,
            ViewGroup parent, int viewType) {
        return null;
    }

    /**
     * Launches the first app in the current search results. The result list at the moment Enter
     * is pressed is used, so a search that finishes later does not change the target.
     */
    @Override
    public boolean launchHighlightedItem() {
        ActivityAllAppsContainerView<?> appsView = mLauncher.getAppsView();
        if (appsView == null) {
            return launchHighlightedView();
        }
        AlphabeticalAppsList apps = appsView.getSearchResultList();
        if (apps == null || !apps.hasSearchResults()) {
            return false;
        }
        List<AdapterItem> items = apps.getAdapterItems();
        for (int i = 0; i < items.size(); i++) {
            ItemInfo itemInfo = items.get(i).itemInfo;
            if (itemInfo == null || itemInfo.getIntent() == null) {
                continue;
            }
            return mLauncher.startActivitySafely(
                    findBoundIcon(appsView, i, itemInfo), itemInfo.getIntent(), itemInfo) != null;
        }
        return false;
    }

    /** Falls back to a previously highlighted icon when search results are unavailable. */
    private boolean launchHighlightedView() {
        if (mHighlightedView instanceof BubbleTextView
                && mHighlightedView.getTag() instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) mHighlightedView.getTag();
            if (itemInfo.getIntent() == null) {
                return false;
            }
            return mLauncher.startActivitySafely(
                    mHighlightedView, itemInfo.getIntent(), itemInfo) != null;
        }
        return false;
    }

    /**
     * Returns the icon already drawn for {@code itemInfo}, so the launch animates from that icon.
     * A recycled row that still shows a different app is ignored.
     */
    @Nullable
    private View findBoundIcon(ActivityAllAppsContainerView<?> appsView, int position,
            ItemInfo itemInfo) {
        if (appsView.getSearchRecyclerView() == null) {
            return null;
        }
        RecyclerView.ViewHolder holder =
                appsView.getSearchRecyclerView().findViewHolderForAdapterPosition(position);
        if (holder == null || !sameItem(holder.itemView.getTag(), itemInfo)) {
            return null;
        }
        return holder.itemView;
    }

    private static boolean sameItem(Object tag, ItemInfo itemInfo) {
        if (!(tag instanceof ItemInfo)) {
            return false;
        }
        ItemInfo shown = (ItemInfo) tag;
        return shown == itemInfo
                || (Objects.equals(shown.user, itemInfo.user)
                && Objects.equals(shown.getTargetComponent(), itemInfo.getTargetComponent()));
    }

    @Override
    public View getHighlightedItem() {
        return mHighlightedView;
    }

    @Override
    public void clearHighlightedItem() {
        mHighlightedView = null;
    }
}
