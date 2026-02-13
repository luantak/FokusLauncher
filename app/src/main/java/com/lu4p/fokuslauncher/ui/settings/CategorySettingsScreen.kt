package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit,
        onEditCategoryApps: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newCategory by remember { mutableStateOf("") }
    val categories = remember(uiState.allApps, uiState.appCategories, uiState.categoryDefinitions) {
        deriveEditableCategories(uiState)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
                title = { Text("App Categories", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
        )
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    singleLine = true,
                    label = { Text("New category") },
                    modifier = Modifier.weight(1f)
            )
            TextButton(
                    onClick = {
                        viewModel.addCategoryDefinition(newCategory)
                        newCategory = ""
                    }
            ) {
                Text("Add")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(categories, key = { it }) { category ->
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { onEditCategoryApps(category) }
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                            text = category,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onEditCategoryApps(category) }) { Text("Edit apps") }
                    IconButton(onClick = { viewModel.deleteCategory(category) }) {
                        Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete category",
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAppsScreen(
        category: String,
        viewModel: SettingsViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortedApps = remember(uiState.allApps) { uiState.allApps.sortedBy { it.label.lowercase() } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
                title = { Text(category, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sortedApps, key = { it.packageName }) { app ->
                val explicit = uiState.appCategories[app.packageName]
                val effectiveCategory = explicit ?: app.category
                val assigned = effectiveCategory.equals(category, ignoreCase = true)
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable {
                                            if (assigned) {
                                                viewModel.setAppCategory(app.packageName, "")
                                            } else {
                                                viewModel.setAppCategory(app.packageName, category)
                                            }
                                        }
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                        )
                        val currentCategory = effectiveCategory.ifBlank { "No category" }
                        Text(
                                text = currentCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Checkbox(
                            checked = assigned,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    viewModel.setAppCategory(app.packageName, category)
                                } else {
                                    viewModel.setAppCategory(app.packageName, "")
                                }
                            }
                    )
                }
            }
        }
    }
}

private fun deriveEditableCategories(uiState: SettingsUiState): List<String> {
    val appCategories =
            uiState.allApps.mapNotNull { app ->
                val category = uiState.appCategories[app.packageName] ?: app.category
                category.takeIf { it.isNotBlank() }
            }
    return (appCategories + uiState.categoryDefinitions).distinct().sorted()
}
