/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.popup

import android.content.Intent
import android.content.pm.LauncherApps
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.android.launcher3.LauncherAppState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.CustomAppNameStore
import com.android.launcher3.views.ActivityContext
import com.android.launcher3.views.DialogScope
import com.android.launcher3.views.DialogViewModel
import com.android.launcher3.views.RenameDialogView
import kotlinx.coroutines.flow.MutableStateFlow

/** Model for showing the shared launcher rename dialog when renaming an app. */
class AppRenameDialogViewModel(
    private val activityContext: ActivityContext,
    private val itemInfo: ItemInfo,
) :
    DialogViewModel<AppRenameDialogViewModel>(
        title = activityContext.asContext().getString(R.string.rename_app_title),
        content = { viewModel -> AppRenameDialogView(viewModel) },
        neutralButton =
            activityContext
                .asContext()
                .getString(R.string.home_screen_files_rename_dialog_neutral_button),
        positiveButton =
            activityContext
                .asContext()
                .getString(R.string.home_screen_files_rename_dialog_positive_button),
        onPositiveButtonClick = { viewModel -> viewModel.submitName() },
    ) {

    val canReset = CustomAppNameStore.getCustomName(activityContext.asContext(), itemInfo) != null

    val name = MutableStateFlow(TextFieldValue(text = itemInfo.title?.toString().orEmpty()))

    fun submitName(): Boolean {
        val newName = name.value.text.trim()
        if (newName.isEmpty() || newName.length > MAX_APP_NAME_LENGTH) {
            return false
        }

        val context = activityContext.asContext()
        val systemTitle = getSystemTitle(itemInfo)
        if (systemTitle != null && newName == systemTitle.toString()) {
            resetToOriginalName()
            return true
        }

        itemInfo.title = newName
        CustomAppNameStore.saveCustomName(context, itemInfo, newName)
        updateWorkspaceItemIfNeeded()
        forceUiUpdate()
        return true
    }

    fun resetToOriginalName() {
        val context = activityContext.asContext()
        CustomAppNameStore.saveCustomName(context, itemInfo, null)
        val systemTitle = getSystemTitle(itemInfo)
        if (systemTitle != null) {
            itemInfo.title = systemTitle
            updateWorkspaceItemIfNeeded()
        }
        forceUiUpdate()
    }

    private fun updateWorkspaceItemIfNeeded() {
        if (itemInfo is WorkspaceItemInfo) {
            activityContext.modelWriter.updateItemInDatabase(itemInfo)
        }
    }

    private fun getSystemTitle(info: ItemInfo): CharSequence? {
        val component = info.targetComponent ?: return null
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setComponent(component)
            }
        val activityInfo =
            activityContext
                .asContext()
                .getSystemService(LauncherApps::class.java)
                ?.resolveActivity(intent, info.user)
        return activityInfo?.let { Utilities.trim(it.label) }
    }

    private fun forceUiUpdate() {
        val component = itemInfo.targetComponent ?: return
        LauncherAppState.getInstance(activityContext.asContext())
            .model
            .onCustomAppNameChanged(component, itemInfo.user)
    }

    companion object {
        const val MAX_APP_NAME_LENGTH = 32
    }
}

@Composable
private fun DialogScope.AppRenameDialogView(viewModel: AppRenameDialogViewModel) {
    Column {
        RenameDialogView(
            viewModel = viewModel,
            name = viewModel.name,
            selectUntilExtension = false,
            maxLength = AppRenameDialogViewModel.MAX_APP_NAME_LENGTH,
        )
        if (viewModel.canReset) {
            TextButton(
                onClick = {
                    viewModel.resetToOriginalName()
                    dismiss(animate = true)
                }
            ) {
                Text(stringResource(R.string.rename_app_reset))
            }
        }
    }
}
