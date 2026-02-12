package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.utils.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppsForCategoryScreen(
    categoryName: String,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val allApps by viewModel.uiState.collectAsStateWithLifecycle()
    val appsInCategory by viewModel.getAppsInCategory(categoryName).collectAsStateWithLifecycle(emptyList())
    
    val appsInCategorySet = remember(appsInCategory) {
        appsInCategory.map { it.packageName }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add apps to $categoryName", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("select_apps_list")
        ) {
            item {
                Text(
                    text = "Select apps to add",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            val filteredApps = allApps.allApps.filter {
                // Don't show private space apps
                it.category != "Private"
            }

            items(filteredApps) { app ->
                val isInCategory = appsInCategorySet.contains(app.packageName)
                var isChecked by remember(app.packageName, isInCategory) {
                    mutableStateOf(isInCategory)
                }

                SelectableAppItem(
                    app = app,
                    isSelected = isChecked,
                    onToggle = {
                        isChecked = !isChecked
                        if (isChecked) {
                            viewModel.addAppToCategory(app.packageName, categoryName)
                        } else {
                            viewModel.removeAppFromCategory(app.packageName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectableAppItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("selectable_app_${app.packageName}")
    ) {
        app.icon?.let {
            androidx.compose.foundation.Image(
                bitmap = androidx.compose.ui.graphics.asImageBitmap(
                    android.graphics.drawable.BitmapDrawable(
                        LocalContext.current.resources,
                        it.toBitmap()
                    ).bitmap
                ),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (app.category.isNotEmpty() && app.category != "Private") {
                Text(
                    text = "Currently in: ${app.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}
