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

package com.android.launcher3.homescreenfiles

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import com.android.launcher3.views.DialogScope
import com.android.launcher3.views.RenameDialogView

/** Composable which renders the content view for the home screen files rename dialog. */
@Composable
fun DialogScope.HomeScreenFilesRenameDialogView(viewModel: HomeScreenFilesRenameDialogViewModel) {
    RenameDialogView(viewModel, viewModel.name, selectUntilExtension = true)
}

// Used to locate nodes in tests.
@VisibleForTesting const val CLEAR_BUTTON_TAG = "clearButton"
@VisibleForTesting const val TEXT_FIELD_TAG = "textField"
