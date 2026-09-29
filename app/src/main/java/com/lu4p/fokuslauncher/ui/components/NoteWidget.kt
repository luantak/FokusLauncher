package com.lu4p.fokuslauncher.ui.components

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound

private const val HOME_NOTE_MAX_VISIBLE_LINES = 10

/**
 * Read-only home note, rendered with [renderNoteMarkdown]. Tapping a task line calls
 * [onToggleTask] with its source line index; long-pressing anywhere in the row calls
 * [onClick], which opens [HomeNoteEditor]. Shows a muted placeholder when [text] is blank.
 */
@Composable
fun NoteWidget(
        text: String,
        modifier: Modifier = Modifier,
        alignment: HomeWidgetAlignment = HomeWidgetAlignment.START,
        outlined: Boolean = false,
        onClick: () -> Unit = {},
        onToggleTask: (lineIndex: Int) -> Unit = {},
) {
    val isEmpty = text.isBlank()
    val baseColor = MaterialTheme.colorScheme.onBackground
    val color = if (isEmpty) baseColor.copy(alpha = 0.38f) else baseColor
    val style = MaterialTheme.typography.bodyLarge
    val placeholder = stringResource(R.string.home_note_placeholder)
    val displayText =
            remember(text, placeholder) {
                if (isEmpty) AnnotatedString(placeholder) else renderNoteMarkdown(text.trimEnd())
            }
    val (boxAlignment, textAlign) =
            when (alignment) {
                HomeWidgetAlignment.START -> Alignment.CenterStart to TextAlign.Start
                HomeWidgetAlignment.CENTER -> Alignment.Center to TextAlign.Center
                HomeWidgetAlignment.END -> Alignment.CenterEnd to TextAlign.End
            }

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var elementSize by remember { mutableStateOf(IntSize.Zero) }
    val lastDown = remember { mutableStateOf(Offset.Unspecified) }
    val taskLines = remember(text) { if (isEmpty) emptyList() else noteTaskLines(text) }

    fun taskLineAt(position: Offset): Int? {
        val layout = textLayout ?: return null
        if (taskLines.isEmpty() || !position.isSpecified) return null
        // With a photo backdrop the text is centered in a padded pill; elsewhere this is zero.
        val inset =
                Offset(
                        (elementSize.width - layout.size.width) / 2f,
                        (elementSize.height - layout.size.height) / 2f,
                )
        val offset = layout.getOffsetForPosition(position - inset)
        val line = noteSourceLineAt(layout.layoutInput.text.text, offset)
        return line.takeIf { it in taskLines }
    }

    val renderedLines = displayText.text.split('\n')
    val toggleActions =
            taskLines.map { line ->
                val label =
                        renderedLines.getOrNull(line).orEmpty().trim().removePrefix("☐ ").removePrefix("☑ ")
                CustomAccessibilityAction(stringResource(R.string.home_note_toggle_task, label)) {
                    onToggleTask(line)
                    true
                }
            }
    val editLabel = stringResource(R.string.home_note_edit_action)

    Box(
            contentAlignment = boxAlignment,
            modifier =
                    modifier.fillMaxWidth().clipToBounds()
                            .combinedClickableWithSystemSound(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onLongClickLabel = editLabel,
                                    onLongClick = onClick,
                                    onClick = {},
                            ),
    ) {
        val textModifier =
                Modifier.onSizeChanged { elementSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                lastDown.value =
                                        awaitFirstDown(
                                                        requireUnconsumed = false,
                                                        pass = PointerEventPass.Initial,
                                                )
                                                .position
                            }
                        }
                        .combinedClickableWithSystemSound(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClickLabel = editLabel,
                                onLongClick = onClick,
                                onClick = {
                                    val line = taskLineAt(lastDown.value)
                                    if (line != null) onToggleTask(line)
                                },
                        )
                        .then(
                                if (toggleActions.isEmpty()) Modifier
                                else Modifier.semantics { customActions = toggleActions }
                        )
                        .testTag("note_widget")
        if (outlined) {
            OutlinedText(
                    text = displayText,
                    style = style,
                    color = color,
                    maxLines = HOME_NOTE_MAX_VISIBLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    onTextLayout = { textLayout = it },
                    modifier = textModifier,
            )
        } else {
            Text(
                    text = displayText,
                    style = style,
                    color = color,
                    maxLines = HOME_NOTE_MAX_VISIBLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    onTextLayout = { textLayout = it },
                    modifier = textModifier,
            )
        }
    }
}

/** Full-screen editor; the owner saves the current draft when the editor is dismissed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeNoteEditor(
        initialText: String,
        draftText: String = initialText,
        onDraftChange: (String) -> Unit = {},
        onDismiss: () -> Unit,
) {
    var value by
            rememberSaveable(initialText, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(draftText, TextRange(draftText.length)))
            }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val hostView = LocalView.current
    val showStatusBar = remember(hostView) {
        ViewCompat.getRootWindowInsets(hostView)?.isVisible(WindowInsetsCompat.Type.statusBars()) == true
    }
    val dismissEditor = {
        keyboard?.hide()
        onDismiss()
    }

    fun updateValue(next: TextFieldValue) {
        value = next
        onDraftChange(next.text)
    }

    Dialog(
            onDismissRequest = dismissEditor,
            properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
            ),
    ) {
        val view = LocalView.current
        val latestDismiss by rememberUpdatedState(dismissEditor)
        DisposableEffect(view, showStatusBar) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                    if (showStatusBar) {
                        show(WindowInsetsCompat.Type.statusBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    } else {
                        hide(WindowInsetsCompat.Type.statusBars())
                        systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    show(WindowInsetsCompat.Type.navigationBars())
                }
            }
            onDispose {}
        }
        DisposableEffect(view) {
            // Give this editor priority over the IME's back callback so one swipe closes both.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val dispatcher = (view.parent as? DialogWindowProvider)?.window?.onBackInvokedDispatcher
                val callback = OnBackInvokedCallback { latestDismiss() }
                dispatcher?.registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback,
                )
                onDispose { dispatcher?.unregisterOnBackInvokedCallback(callback) }
            } else {
                onDispose {}
            }
        }
        Surface(
                modifier = Modifier.fillMaxSize().onPreInterceptKeyBeforeSoftKeyboard { event ->
                    if (event.key == Key.Back) {
                        if (event.type == KeyEventType.KeyUp && !event.nativeKeyEvent.isCanceled) {
                            dismissEditor()
                        }
                        true
                    } else {
                        false
                    }
                },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f),
        ) {
            Column(
                    modifier = Modifier.fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                            .navigationBarsPadding().imePadding()
                            .padding(horizontal = 16.dp).padding(bottom = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FokusIconButton(
                            onClick = dismissEditor,
                            modifier = Modifier.testTag("note_edit_back"),
                    ) {
                        LauncherIcon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                            stringResource(R.string.home_note_edit_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                    )
                }
                TextField(
                        value = value,
                        onValueChange = { updateValue(updateNoteEditorValue(value, it)) },
                        colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                                .focusRequester(focusRequester).testTag("note_edit_field"),
                )
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    FokusIconButton(
                            onClick = {
                                val (text, cursor) = insertNoteTaskLine(value.text, value.selection.max)
                                updateValue(TextFieldValue(text, TextRange(cursor)))
                                focusRequester.requestFocus()
                                keyboard?.show()
                            },
                            modifier = Modifier.testTag("note_add_task"),
                    ) {
                        LauncherIcon(
                                Icons.Default.Add,
                                stringResource(R.string.home_note_add_task),
                                tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                }
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}
