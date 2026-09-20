/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.views

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.launcher3.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Shared text field used by the home screen files rename dialog and the app rename dialog.
 *
 * @param selectUntilExtension when true, initial focus selects only the name before a file
 *   extension, matching the files/folder rename behavior.
 */
@Composable
fun <T : DialogViewModel<T>> DialogScope.RenameDialogView(
    viewModel: T,
    name: MutableStateFlow<TextFieldValue>,
    selectUntilExtension: Boolean = false,
    maxLength: Int? = null,
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val textFieldValue by name.collectAsStateWithLifecycle()

    // Apply focus selection early to prevent flicker when the text field receives initial focus.
    LaunchedEffect(Unit) { name.value = name.value.focus(selectUntilExtension) }

    OutlinedTextField(
        modifier =
            Modifier.testTag(TEXT_FIELD_TAG)
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester)
                .onFocusChanged { name.value = name.value.apply(it, selectUntilExtension) },
        value = textFieldValue,
        onValueChange = { value ->
            name.value =
                if (maxLength != null && value.text.length > maxLength) {
                    value.copy(text = value.text.take(maxLength))
                } else {
                    value
                }
        },
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (viewModel.onPositiveButtonClick?.invoke(viewModel) == true) {
                        dismiss(animate = true)
                    }
                }
            ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        singleLine = true,
        trailingIcon = {
            IconButton(
                modifier = Modifier.testTag(CLEAR_BUTTON_TAG),
                onClick = {
                    name.value = TextFieldValue()
                    textFieldFocusRequester.requestFocus()
                },
            ) {
                Icon(
                    painterResource(R.drawable.ic_home_screen_files_rename_dialog_clear),
                    stringResource(R.string.home_screen_files_rename_dialog_clear_button),
                )
            }
        },
    )

    // The text field should receive initial focus.
    LaunchedEffect(Unit) { textFieldFocusRequester.requestFocus() }
}

// Used to locate nodes in tests.
@VisibleForTesting const val CLEAR_BUTTON_TAG = "clearButton"
@VisibleForTesting const val TEXT_FIELD_TAG = "textField"

/** Text selection should be applied/cleared on focus/blur. */
private fun TextFieldValue.apply(state: FocusState, selectUntilExtension: Boolean): TextFieldValue =
    if (state.isFocused) focus(selectUntilExtension) else blur()

/** Text selection should be cleared on blur. */
private fun TextFieldValue.blur(): TextFieldValue = copy(selection = TextRange.Zero)

/**
 * Text selection should be applied on focus. If [selectUntilExtension] is true and a file extension
 * is present, do not include it in the selection so that the user can more quickly rename a file
 * without accidentally changing its extension.
 */
private fun TextFieldValue.focus(selectUntilExtension: Boolean): TextFieldValue {
    val end =
        if (selectUntilExtension) {
            val extensionIndex = text.lastIndexOf(".")
            if (extensionIndex != -1) extensionIndex else text.length
        } else {
            text.length
        }
    return copy(selection = TextRange(0, end))
}
