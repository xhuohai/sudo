package io.github.xhuohai.sudo.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.xhuohai.sudo.data.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTopicScreen(
    onBackClick: () -> Unit,
    onTopicCreated: (topicId: Int, slug: String) -> Unit,
    viewModel: CreateTopicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successTopicId) {
        uiState.successTopicId?.let { topicId ->
            onTopicCreated(topicId, uiState.successTopicSlug ?: "")
            viewModel.resetSuccess()
        }
    }

    LaunchedEffect(uiState.categories) {
        if (selectedCategory == null && uiState.categories.isNotEmpty()) {
            // Default to first category or "General" if found, logic can be improved
            selectedCategory = uiState.categories.firstOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建话题") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (selectedCategory != null) {
                                viewModel.createTopic(title, content, selectedCategory!!.id)
                            }
                        },
                        enabled = !uiState.isSending && title.isNotBlank() && content.isNotBlank() && selectedCategory != null
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "发布")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isSending
                )
                
                // Category Selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "选择分类",
                        onValueChange = {},
                        label = { Text("分类") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, "选择")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!uiState.isSending) expandedCategoryDropdown = true },
                        enabled = false, // Disable typing, handled by box click effectively? 
                        // Actually better to use a clickable Box over input or DropdownMenu properly
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    // Overlay clickable
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !uiState.isSending) { expandedCategoryDropdown = true }
                    )

                    DropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容 (支持 Markdown)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Fill remaining space? No, inside ScrollView weight doesn't work well without height constraint.
                        .itemHeight(300.dp), // Check if custom modifier needed or just height
                    minLines = 10,
                    enabled = !uiState.isSending
                )
                
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// Helper extension if simple height doesn't work
private fun Modifier.itemHeight(height: androidx.compose.ui.unit.Dp) = this.then(Modifier.padding(vertical = 8.dp).height(height))
