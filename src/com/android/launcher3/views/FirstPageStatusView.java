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

package com.android.launcher3.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.util.ApiWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for swipeable first-page status content.
 */
public class FirstPageStatusView extends FrameLayout {

    private final ArrayList<View> mPages = new ArrayList<>();
    private final PagerAdapter mAdapter = new PagerAdapter();
    private final FirstPageCompactStatusView mCompactStatusView;
    private final FirstPageMediaStatusView mMediaStatusView;
    @Nullable
    private final ApiWrapper.MediaDataProvider mMediaDataProvider;

    private RecyclerView mPager;
    private LinearLayoutManager mLayoutManager;
    private PageIndicatorDots mPageIndicator;
    private PagerSnapHelper mSnapHelper;
    private int mCurrentPage;

    public FirstPageStatusView(Context context) {
        this(context, null);
    }

    public FirstPageStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FirstPageStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.first_page_status_view, this, true);
        mCompactStatusView = new FirstPageCompactStatusView(context);
        mMediaStatusView = new FirstPageMediaStatusView(context);
        mMediaDataProvider = ApiWrapper.INSTANCE.get(context).createMediaDataProvider();
        bindViews();
        rebuildPages();
    }

    private void bindViews() {
        mPager = findViewById(R.id.first_page_status_pager);
        mPageIndicator = findViewById(R.id.first_page_status_page_indicator);
        mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        mPager.setLayoutManager(mLayoutManager);
        mPager.setAdapter(mAdapter);
        mPager.setItemAnimator(null);
        mPager.setOverScrollMode(OVER_SCROLL_NEVER);
        mPager.setNestedScrollingEnabled(false);

        mSnapHelper = new PagerSnapHelper();
        mSnapHelper.attachToRecyclerView(mPager);
        mPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateCurrentPage(findSnappedPage());
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mMediaDataProvider != null) {
            mMediaDataProvider.setCallback(this::onMediaInfoUpdated);
            mMediaDataProvider.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMediaDataProvider != null) {
            mMediaDataProvider.setCallback(null);
            mMediaDataProvider.stop();
        }
    }

    private void setPages(List<View> pages) {
        int previousPage = Math.min(mCurrentPage, Math.max(0, pages.size() - 1));
        mPages.clear();
        mPages.addAll(pages);
        mAdapter.notifyDataSetChanged();
        updatePagerUi();
        if (!mPages.isEmpty()) {
            mPager.scrollToPosition(previousPage);
            updateCurrentPage(previousPage);
        }
    }

    private void updatePagerUi() {
        int pageCount = mPages.size();
        mPageIndicator.setMarkersCount(pageCount);
        mPageIndicator.setVisibility(pageCount > 1 ? VISIBLE : GONE);
        mPager.setHorizontalScrollBarEnabled(pageCount > 1);
    }

    private int findSnappedPage() {
        View snapView = mSnapHelper.findSnapView(mLayoutManager);
        if (snapView == null) {
            return mCurrentPage;
        }
        int position = mLayoutManager.getPosition(snapView);
        return position == RecyclerView.NO_POSITION ? mCurrentPage : position;
    }

    private void updateCurrentPage(int page) {
        int clampedPage = Math.max(0, Math.min(page, Math.max(0, mPages.size() - 1)));
        mCurrentPage = clampedPage;
        mPageIndicator.setActiveMarker(clampedPage);
    }

    private void onMediaInfoUpdated(@Nullable ApiWrapper.MediaInfo mediaInfo) {
        boolean hadMedia = mMediaStatusView.hasMedia();
        mMediaStatusView.setMediaInfo(mediaInfo);
        if (hadMedia != mMediaStatusView.hasMedia()) {
            rebuildPages();
        }
    }

    private void rebuildPages() {
        ArrayList<View> pages = new ArrayList<>();
        pages.add(mCompactStatusView);
        if (mMediaStatusView.hasMedia()) {
            pages.add(mMediaStatusView);
        }
        setPages(pages);
    }

    private final class PagerAdapter extends RecyclerView.Adapter<PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new PageViewHolder(container);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            View page = mPages.get(position);
            if (page.getParent() instanceof ViewGroup) {
                ((ViewGroup) page.getParent()).removeView(page);
            }
            holder.container.removeAllViews();
            holder.container.addView(page);
        }

        @Override
        public int getItemCount() {
            return mPages.size();
        }
    }

    private static final class PageViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout container;

        private PageViewHolder(@NonNull FrameLayout container) {
            super(container);
            this.container = container;
        }
    }
}
