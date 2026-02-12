package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.data.model.CategoryConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onEditCategory: (String) -> Unit = {}
) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", color = MaterialTheme.colorScheme.onBackground) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newCategoryName = ""
                    errorMessage = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_category")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("categories_list")
        ) {
            item {
                Text(
                    text = "Custom categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            items(customCategories) { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditCategory(category.name) }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag("category_item_${category.name}")
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            if (customCategories.isEmpty()) {
                item {
                    Text(
                        text = "No custom categories yet. Tap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                    )
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Category") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = {
                                newCategoryName = it
                                errorMessage = null
                            },
                            label = { Text("Category name") },
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_category_input")
                        )
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newCategoryName.trim()
                            when {
                                trimmed.isEmpty() -> {
                                    errorMessage = "Category name cannot be empty"
                                }
                                CategoryConstants.isSystemCategory(trimmed) -> {
                                    errorMessage = "This category name is reserved"
                                }
                                else -> {
                                    viewModel.createCategory(trimmed) { success, error ->
                                        if (success) {
                                            showCreateDialog = false
                                        } else {
                                            errorMessage = error ?: "Failed to create category"
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
