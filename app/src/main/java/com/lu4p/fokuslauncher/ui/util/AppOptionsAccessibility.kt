package com.lu4p.fokuslauncher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.lu4p.fokuslauncher.R

@Composable
internal fun Modifier.appOptionsAccessibilityAction(onOpen: () -> Unit): Modifier {
    val label = stringResource(R.string.action_app_options)
    return semantics {
        customActions = listOf(CustomAccessibilityAction(label) {
            onOpen()
            true
        })
    }
}