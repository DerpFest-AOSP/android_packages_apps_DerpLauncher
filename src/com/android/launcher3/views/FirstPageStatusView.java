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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
    private final int mTouchSlop;
    private float mDownX;
    private float mDownY;
    private boolean mGestureSettled;
    private boolean mPassTouchToParent;
    private boolean mForwardingToPager;

    public FirstPageStatusView(Context context) {
        this(context, null);
    }

    public FirstPageStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FirstPageStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPages.size() <= 1) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mGestureSettled = false;
                mPassTouchToParent = false;
                mForwardingToPager = false;
                // Claim the gesture before Workspace, which otherwise wins the touch-slop race
                // and turns this swipe into a home-screen or Google Now page change.
                requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mGestureSettled) {
                    settleGesture(ev);
                }
                if (mPassTouchToParent) {
                    if (mForwardingToPager) {
                        forwardCancelToPager(ev);
                        mForwardingToPager = false;
                    }
                    requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mPassTouchToParent = false;
                mForwardingToPager = false;
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mPages.size() <= 1) {
            return super.onTouchEvent(ev);
        }
        // Empty parts of the cell never hit the pager, so this view keeps the gesture. Settle it
        // here as well: once a child misses ACTION_DOWN, later moves skip onInterceptTouchEvent.
        if (ev.getActionMasked() == MotionEvent.ACTION_MOVE && !mGestureSettled) {
            settleGesture(ev);
        }
        if (mPassTouchToParent) {
            if (mForwardingToPager) {
                forwardCancelToPager(ev);
                mForwardingToPager = false;
            }
            requestDisallowInterceptTouchEvent(false);
            return true;
        }
        // Touches that miss the text still belong to this row. Send them to the pager so the
        // whole cell, not only the date and weather, changes pages.
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mForwardingToPager = false;
        } else {
            mForwardingToPager = true;
        }
        return forwardToPager(ev);
    }

    private void settleGesture(MotionEvent ev) {
        float dx = ev.getX() - mDownX;
        float dy = ev.getY() - mDownY;
        if (Math.hypot(dx, dy) < mTouchSlop) {
            return;
        }
        mGestureSettled = true;
        boolean horizontal = Math.abs(dx) > Math.abs(dy);
        mPassTouchToParent = !horizontal || !pagerCanScroll(dx);
    }

    private boolean pagerCanScroll(float dx) {
        boolean towardEnd = dx < 0;
        if (mPager.canScrollHorizontally(towardEnd ? 1 : -1)) {
            return true;
        }
        return towardEnd ? mCurrentPage < mPages.size() - 1 : mCurrentPage > 0;
    }

    private boolean forwardToPager(MotionEvent ev) {
        if (mPager.getWidth() <= 0 || mPager.getHeight() <= 0) {
            return false;
        }
        int[] parentLocation = new int[2];
        int[] pagerLocation = new int[2];
        getLocationInWindow(parentLocation);
        mPager.getLocationInWindow(pagerLocation);
        float localX = ev.getX() + parentLocation[0] - pagerLocation[0];
        float localY = ev.getY() + parentLocation[1] - pagerLocation[1];
        localX = Math.max(0f, Math.min(localX, mPager.getWidth() - 1));
        localY = Math.max(0f, Math.min(localY, mPager.getHeight() - 1));
        MotionEvent forwarded = MotionEvent.obtain(ev);
        forwarded.setLocation(localX, localY);
        boolean handled = mPager.dispatchTouchEvent(forwarded);
        forwarded.recycle();
        return handled;
    }

    private void forwardCancelToPager(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        forwardToPager(cancel);
        cancel.recycle();
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            updatePagerHeight();
        }
    }

    private void setPages(List<View> pages) {
        int previousPage = Math.min(mCurrentPage, Math.max(0, pages.size() - 1));
        mPages.clear();
        mPages.addAll(pages);
        mAdapter.notifyDataSetChanged();
        updatePagerUi();
        updatePagerHeight();
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
        mCompactStatusView.setForceTwoLineLayout(mMediaStatusView.hasMedia());
        updatePagerHeight();
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

    private void updatePagerHeight() {
        mPager.post(() -> {
            int availableWidth = mPager.getWidth();
            if (availableWidth <= 0) {
                return;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(
                    availableWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(
                    0, View.MeasureSpec.UNSPECIFIED);
            int maxHeight = 0;
            for (View page : mPages) {
                page.measure(widthSpec, heightSpec);
                maxHeight = Math.max(maxHeight, page.getMeasuredHeight());
            }
            ViewGroup.LayoutParams layoutParams = mPager.getLayoutParams();
            if (layoutParams.height != maxHeight && maxHeight > 0) {
                layoutParams.height = maxHeight;
                mPager.setLayoutParams(layoutParams);
            }
        });
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
