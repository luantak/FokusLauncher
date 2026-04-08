package com.lu4p.fokuslauncher.ui.settings.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.util.VerticalSlotReorderState
import com.lu4p.fokuslauncher.ui.util.verticalReorderDragHandle

@Composable
fun EditorSectionHeader(@StringRes textRes: Int) {
    Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun EditorDragHandleReorderIcon(
        reorderState: VerticalSlotReorderState,
        index: Int,
        lastIndex: Int,
        onReorder: (from: Int, to: Int) -> Unit,
        onReset: () -> Unit,
        vararg pointerInputKeys: Any?,
) {
    Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.cd_drag_to_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                    Modifier.size(24.dp)
                            .verticalReorderDragHandle(
                                    reorderState,
                                    index,
                                    lastIndex,
                                    onReorder,
                                    onReset,
                                    *pointerInputKeys,
                            ),
    )
}

@Composable
fun ProfileBadgeSubtitle(profileBadge: String?) {
    profileBadge?.let { badge ->
        Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun RowScope.EditorUncheckedLeadingSpacers() {
    Spacer(modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.width(8.dp))
}

/**
 * Checkbox with the standard 8.dp gaps used on editor list rows; [content] is the rest of the row
 * (e.g. a weighted [androidx.compose.foundation.layout.Column]).
 */
@Composable
fun RowScope.EditorStandardCheckboxGutter(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        content: @Composable RowScope.() -> Unit,
) {
    Spacer(modifier = Modifier.width(8.dp))
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Spacer(modifier = Modifier.width(8.dp))
    content()
}
