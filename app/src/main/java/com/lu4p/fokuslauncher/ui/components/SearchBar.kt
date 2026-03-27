package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Minimal search bar with a blinking cursor and no background decoration.
 * Matches the minimalist aesthetic of the launcher.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    /** Invoked when the user presses Go/Done/Search on the IME (keyboard “enter”). */
    onImeAction: (() -> Unit)? = null,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                imeAction = if (onImeAction != null) ImeAction.Go else ImeAction.Default
            ),
        keyboardActions =
            KeyboardActions(
                onGo = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() },
                onSearch = { onImeAction?.invoke() }
            ),
        decorationBox = { innerTextField ->
            if (query.isEmpty()) {
                androidx.compose.material3.Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
            innerTextField()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier
            )
    )
}
