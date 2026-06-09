/*
 * Copyright (C) 2026 The uwuAOSP Project
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

package com.android.launcher3.smartspacer

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.android.launcher3.R
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants.SMARTSPACER_PACKAGE_NAME
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LauncherSmartspacerContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val client = SmartspacerClient.getInstance(context)

    private lateinit var fallbackView: View
    private var smartspacerView: View? = null
    private var scope: CoroutineScope? = null
    private var refreshJob: Job? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        fallbackView = findViewById(R.id.launcher_smartspacer_fallback)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (scope == null) {
            scope = MainScope()
        }
        refresh()
    }

    override fun onDetachedFromWindow() {
        refreshJob?.cancel()
        refreshJob = null
        scope?.cancel()
        scope = null
        clearSmartspacerView()
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            refresh()
        }
    }

    private fun refresh() {
        val localScope = scope ?: return
        refreshJob?.cancel()
        if (!isSmartspacerInstalled()) {
            showFallback()
            return
        }
        refreshJob = localScope.launch {
            val permissionState = client.checkCallingPermission()
            if (!isActive) return@launch
            if (permissionState == null) {
                showFallback()
            } else {
                showSmartspacer()
            }
        }
    }

    private fun showSmartspacer() {
        ensureSmartspacerView().visibility = View.VISIBLE
        fallbackView.visibility = View.GONE
    }

    private fun showFallback() {
        clearSmartspacerView()
        fallbackView.visibility = View.VISIBLE
    }

    private fun ensureSmartspacerView(): View {
        return smartspacerView
            ?: LayoutInflater.from(context)
                .inflate(R.layout.launcher_smartspacer_view, this, false)
                .also {
                    addView(it, 0)
                    smartspacerView = it
                }
    }

    private fun clearSmartspacerView() {
        smartspacerView?.let {
            removeView(it)
            smartspacerView = null
        }
    }

    private fun isSmartspacerInstalled(): Boolean {
        return try {
            context.packageManager.getApplicationInfo(SMARTSPACER_PACKAGE_NAME, 0).enabled
        } catch (_: NameNotFoundException) {
            false
        }
    }
}
