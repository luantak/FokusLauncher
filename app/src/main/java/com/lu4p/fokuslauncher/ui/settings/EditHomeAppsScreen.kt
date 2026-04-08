package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.drawerOpenCountKey
import com.lu4p.fokuslauncher.ui.components.EditorScreenScaffold
import com.lu4p.fokuslauncher.ui.drawer.DrawerProfileSectionUi
import com.lu4p.fokuslauncher.ui.drawer.groupAppsIntoProfileSections
import com.lu4p.fokuslauncher.ui.drawer.profileGroupedAppItems
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForApp
import com.lu4p.fokuslauncher.ui.drawer.profileOriginLabelForFavorite
import com.lu4p.fokuslauncher.ui.drawer.sortAppsAlphabeticallyByProfileSection
import com.lu4p.fokuslauncher.ui.home.HomeViewModel
import com.lu4p.fokuslauncher.ui.settings.components.EditorDragHandleReorderIcon
import com.lu4p.fokuslauncher.ui.settings.components.EditorSectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.EditorStandardCheckboxGutter
import com.lu4p.fokuslauncher.ui.settings.components.EditorUncheckedLeadingSpacers
import com.lu4p.fokuslauncher.ui.settings.components.ProfileBadgeSubtitle
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.ui.util.rememberBooleanChangeWithSystemSound
import com.lu4p.fokuslauncher.ui.util.rememberVerticalSlotReorderState
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHomeAppsScreen(
        viewModel: HomeViewModel,
        onNavigateBack: () -> Unit,
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur
) {
    val editFavorites by viewModel.editFavorites.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val saveAndBack: () -> Unit = {
        viewModel.saveEditedFavorites()
        onNavigateBack()
    }

    EditorScreenScaffold(
            titleText = stringResource(R.string.edit_home_title),
            searchPlaceholderResId = R.string.search_apps,
            backgroundScrim = backgroundScrim,
            listReadyToScroll = allApps.isNotEmpty(),
            onNavigateBack = saveAndBack,
            onDone = saveAndBack,
    ) { searchQuery, listState ->
        val checkedKeys =
                remember(editFavorites) {
                    editFavorites.map { drawerOpenCountKey(it.packageName, it.profileKey) }.toSet()
                }
        val uncheckedApps =
                remember(allApps, checkedKeys, searchQuery) {
                    allApps
                            .filter { drawerOpenCountKey(it.packageName, it.userHandle) !in checkedKeys }
                            .let { list ->
                                if (searchQuery.isBlank()) list
                                else list.filter { it.label.containsNormalizedSearch(searchQuery) }
                            }
                }
        val context = LocalContext.current
        val uncheckedSections =
                remember(uncheckedApps, context) {
                    groupAppsIntoProfileSections(
                            context,
                            uncheckedApps,
                            ::sortAppsAlphabeticallyByProfileSection
                    )
                }

        ReorderableEditHomeAppsList(
                listState = listState,
                editFavorites = editFavorites,
                uncheckedSections = uncheckedSections,
                allApps = allApps,
                onToggle = { viewModel.toggleAppOnHomeScreen(it) },
                onReorder = { from, to -> viewModel.reorderFavorite(from, to) }
        )
    }
}

@Composable
private fun ReorderableEditHomeAppsList(
        listState: LazyListState,
        editFavorites: List<FavoriteApp>,
        uncheckedSections: List<DrawerProfileSectionUi>,
        allApps: List<AppInfo>,
        onToggle: (AppInfo) -> Unit,
        onReorder: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val reorderState = rememberVerticalSlotReorderState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        if (editFavorites.isNotEmpty()) {
            item(key = "header_checked") { EditorSectionHeader(R.string.edit_home_section_on_home) }
        }

        items(
                count = editFavorites.size,
                key = {
                    val fav = editFavorites[it]
                    "checked_${drawerOpenCountKey(fav.packageName, fav.profileKey)}"
                }
        ) { index ->
            val fav = editFavorites[index]
            val matchingApp =
                    remember(fav.packageName, fav.profileKey, allApps) {
                        allApps.find {
                            it.packageName == fav.packageName &&
                                    appProfileKey(it.userHandle) == fav.profileKey
                        }
                    }
            val profileBadge =
                    remember(fav, matchingApp, context) {
                        profileOriginLabelForFavorite(context, fav, matchingApp)
                    }
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .graphicsLayer { translationY = reorderState.translationYForIndex(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EditorDragHandleReorderIcon(
                        reorderState = reorderState,
                        index = index,
                        lastIndex = editFavorites.lastIndex,
                        onReorder = onReorder,
                        onReset = { reorderState.reset() },
                        fav.packageName,
                        fav.profileKey,
                        editFavorites.size,
                )
                EditorStandardCheckboxGutter(
                        checked = true,
                        onCheckedChange =
                                rememberBooleanChangeWithSystemSound {
                                    allApps.find {
                                        it.packageName == fav.packageName &&
                                                appProfileKey(it.userHandle) == fav.profileKey
                                    }?.let { onToggle(it) }
                                },
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = fav.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        ProfileBadgeSubtitle(profileBadge)
                    }
                }
            }
        }

        item(key = "header_unchecked") { EditorSectionHeader(R.string.edit_home_section_all_apps) }

        profileGroupedAppItems(
                sections = uncheckedSections,
                keyPrefix = "unchecked",
                horizontalPadding = 16.dp,
        ) { app ->
            val profileBadge =
                    remember(app.packageName, app.componentName, app.userHandle, context) {
                        profileOriginLabelForApp(context, app)
                    }
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                EditorUncheckedLeadingSpacers()
                EditorStandardCheckboxGutter(
                        checked = false,
                        onCheckedChange = rememberBooleanChangeWithSystemSound { onToggle(app) },
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        ProfileBadgeSubtitle(profileBadge)
                    }
                }
            }
        }

        item(key = "edit_home_list_bottom_spacer") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
