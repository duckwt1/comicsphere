package com.android.dacs3.presentations.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.viewmodel.AdminViewModel

// Định nghĩa màu sắc cho chủ đề trắng đen
private val BlackWhiteTheme = object {
    val primary = Color.Black
    val onPrimary = Color.White
    val background = Color.White
    val surface = Color.White
    val onSurface = Color.Black
    val border = Color.Black
    val divider = Color.LightGray
    val iconTint = Color.Black
    val vipIndicator = Color(0xFFFFD700) // Gold color for VIP
    val adminIndicator = Color.Black
    val error = Color.Red
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMangaManagementScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val mangas by viewModel.mangas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedManga by remember { mutableStateOf<MangaData?>(null) }
    var showMangaDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditMangaDialog by remember { mutableStateOf(false) }
    var showAddMangaDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manga Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlackWhiteTheme.primary,
                    titleContentColor = BlackWhiteTheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BlackWhiteTheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMangaDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Manga",
                            tint = BlackWhiteTheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMangaDialog = true },
                containerColor = BlackWhiteTheme.primary,
                contentColor = BlackWhiteTheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Manga")
            }
        }
    ) { innerPadding ->
        // Sử dụng Box để chứa toàn bộ nội dung và cho phép cuộn
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BlackWhiteTheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search bar - giữ cố định ở trên cùng
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp)),
                    placeholder = { Text("Search manga...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BlackWhiteTheme.iconTint
                        )
                    },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = BlackWhiteTheme.iconTint
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = BlackWhiteTheme.primary,
                        focusedBorderColor = BlackWhiteTheme.primary,
                        unfocusedBorderColor = BlackWhiteTheme.border,
                        focusedLabelColor = BlackWhiteTheme.primary,
                        unfocusedLabelColor = BlackWhiteTheme.border
                    )
                )

                // Nội dung chính có thể cuộn
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BlackWhiteTheme.primary)
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "An error occurred",
                            color = BlackWhiteTheme.error
                        )
                    }
                } else {
                    val filteredMangas = viewModel.getFilteredMangas()

                    if (filteredMangas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.searchQuery.isEmpty())
                                    "No manga found"
                                else
                                    "No manga matching '${viewModel.searchQuery}'",
                                color = BlackWhiteTheme.divider
                            )
                        }
                    } else {
                        // Sử dụng LazyColumn để hiển thị danh sách manga có thể cuộn
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredMangas) { manga ->
                                MangaListItem(
                                    manga = manga,
                                    onClick = {
                                        selectedManga = manga
                                        showMangaDetailsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manga details dialog
    if (showMangaDetailsDialog && selectedManga != null) {
        MangaDetailsDialog(
            manga = selectedManga!!,
            onDismiss = { showMangaDetailsDialog = false },
            onDeleteManga = {
                showMangaDetailsDialog = false
                showDeleteConfirmDialog = true
            },
            onEditManga = {
                showEditMangaDialog = true
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && selectedManga != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = BlackWhiteTheme.surface,
            titleContentColor = BlackWhiteTheme.onSurface,
            textContentColor = BlackWhiteTheme.onSurface,
            title = { Text("Delete Manga") },
            text = { Text("Are you sure you want to delete this manga? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteManga(selectedManga!!.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.error,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BlackWhiteTheme.primary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(BlackWhiteTheme.border)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add manga dialog
    if (showAddMangaDialog) {
        AddMangaDialog(
            onDismiss = { showAddMangaDialog = false },
            onAdd = { title, description, coverUrl, status, tags ->
                viewModel.addManga(title, description, coverUrl, status, tags)
                showAddMangaDialog = false
            }
        )
    }

    // Edit manga dialog
    if (showEditMangaDialog && selectedManga != null) {
        EditMangaDialog(
            manga = selectedManga!!,
            onDismiss = { showEditMangaDialog = false },
            onSave = { title, description, coverUrl, status, tags ->
                viewModel.updateManga(selectedManga!!.id, title, description, coverUrl, status, tags)
                showEditMangaDialog = false
                showMangaDetailsDialog = false
            }
        )
    }
}

@Composable
fun MangaListItem(
    manga: MangaData,
    onClick: () -> Unit
) {
    val title = manga.attributes.title["en"] ?: "Unknown Title"
    val coverUrl = manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlackWhiteTheme.surface
        ),
        border = BorderStroke(1.dp, BlackWhiteTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Manga info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BlackWhiteTheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Status: ${manga.attributes.status ?: "Unknown"}",
                    fontSize = 14.sp,
                    color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val description = manga.attributes.description["en"] ?: ""
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = BlackWhiteTheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = BlackWhiteTheme.iconTint
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MangaDetailsDialog(
    manga: MangaData,
    onDismiss: () -> Unit,
    onDeleteManga: () -> Unit,
    onEditManga: () -> Unit
) {
    val title = manga.attributes.title["en"] ?: "Unknown Title"
    val description = manga.attributes.description["en"] ?: ""
    val status = manga.attributes.status ?: "Unknown"
    val coverUrl = manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName ?: ""

    // Sử dụng Dialog thay vì AlertDialog để có thể tùy chỉnh nhiều hơn
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manga Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlackWhiteTheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BlackWhiteTheme.iconTint
                        )
                    }
                }

                Divider(color = BlackWhiteTheme.divider, modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Cover image and basic info
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Cover image
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = title,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                // Basic info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BlackWhiteTheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Status: $status",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Description
                        item {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BlackWhiteTheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = BlackWhiteTheme.surface
                                ),
                                border = BorderStroke(1.dp, BlackWhiteTheme.divider),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (description.isNotEmpty()) description else "No description available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BlackWhiteTheme.onSurface.copy(alpha = if (description.isNotEmpty()) 0.8f else 0.5f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Tags
                        item {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BlackWhiteTheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val tags = manga.attributes.tags.mapNotNull { it.attributes.name["en"] }

                            if (tags.isEmpty()) {
                                Text(
                                    text = "No tags available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BlackWhiteTheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = BlackWhiteTheme.primary.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, BlackWhiteTheme.border)
                                        ) {
                                            Text(
                                                text = tag,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                color = BlackWhiteTheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = BlackWhiteTheme.divider, modifier = Modifier.padding(vertical = 8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delete button
                    OutlinedButton(
                        onClick = onDeleteManga,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.error
                        ),
                        border = BorderStroke(1.dp, BlackWhiteTheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = BlackWhiteTheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = BlackWhiteTheme.error)
                    }

                    // Edit button
                    Button(
                        onClick = onEditManga,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMangaDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, description: String, coverUrl: String, status: String, tagIds: List<String>) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ongoing") }
    
    // Sử dụng Set để lưu trữ các tagId đã chọn
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }

    val statusOptions = listOf("ongoing", "completed", "hiatus", "cancelled")
    var statusMenuExpanded by remember { mutableStateOf(false) }
    
    // Lấy danh sách tags từ ViewModel
    val firestoreTags by viewModel.firestoreTags.collectAsState()
    
    // Nhóm tags theo group để hiển thị
    val groupedTags = firestoreTags.groupBy { it.group }

    // Đảm bảo tags đã được load
    LaunchedEffect(Unit) {
        if (firestoreTags.isEmpty()) {
            viewModel.fetchTagsFromFirestore()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Manga",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlackWhiteTheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BlackWhiteTheme.iconTint
                        )
                    }
                }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Cover URL
                    item {
                        OutlinedTextField(
                            value = coverUrl,
                            onValueChange = { coverUrl = it },
                            label = { Text("Cover Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Status dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = status,
                                onValueChange = { },
                                label = { Text("Status") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { statusMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Status",
                                            tint = BlackWhiteTheme.iconTint
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    cursorColor = BlackWhiteTheme.primary,
                                    focusedBorderColor = BlackWhiteTheme.primary,
                                    unfocusedBorderColor = BlackWhiteTheme.border,
                                    focusedLabelColor = BlackWhiteTheme.primary,
                                    unfocusedLabelColor = BlackWhiteTheme.border
                                )
                            )

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                                modifier = Modifier.background(BlackWhiteTheme.surface)
                            ) {
                                statusOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = BlackWhiteTheme.onSurface) },
                                        onClick = {
                                            status = option
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Tags section
                    item {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Selected tags: ${selectedTagIds.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Display tags grouped by category
                    groupedTags.forEach { (group, tagsInGroup) ->
                        item {
                            Text(
                                text = group.capitalize(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BlackWhiteTheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tagsInGroup.forEach { tag ->
                                    val isSelected = selectedTagIds.contains(tag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTagIds = if (isSelected) {
                                                selectedTagIds - tag.id
                                            } else {
                                                selectedTagIds + tag.id
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = tag.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (isSelected) BlackWhiteTheme.primary else BlackWhiteTheme.surface,
                                            labelColor = if (isSelected) BlackWhiteTheme.onPrimary else BlackWhiteTheme.onSurface
                                        ),

                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(BlackWhiteTheme.border)
                        )
                    ) {
                        Text("Cancel")
                    }

                    // Add button
                    Button(
                        onClick = {
                            onAdd(title, description, coverUrl, status, selectedTagIds.toList())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        ),
                        enabled = title.isNotEmpty() && coverUrl.isNotEmpty()
                    ) {
                        Text("Add Manga")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMangaDialog(
    manga: MangaData,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, coverUrl: String, status: String, tagIds: List<String>) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val initialTitle = manga.attributes.title["en"] ?: ""
    val initialDescription = manga.attributes.description["en"] ?: ""
    val initialCoverUrl = manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName ?: ""
    val initialStatus = manga.attributes.status ?: "ongoing"
    
    // Lấy danh sách tagIds từ manga
    val initialTagIds = manga.attributes.tags.map { it.id }

    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var coverUrl by remember { mutableStateOf(initialCoverUrl) }
    var status by remember { mutableStateOf(initialStatus) }
    
    // Sử dụng Set để lưu trữ các tagId đã chọn
    var selectedTagIds by remember { mutableStateOf(initialTagIds.toSet()) }

    val statusOptions = listOf("ongoing", "completed", "hiatus", "cancelled")
    var statusMenuExpanded by remember { mutableStateOf(false) }
    
    // Lấy danh sách tags từ ViewModel
    val firestoreTags by viewModel.firestoreTags.collectAsState()
    
    // Nhóm tags theo group để hiển thị
    val groupedTags = firestoreTags.groupBy { it.group }

    // Đảm bảo tags đã được load
    LaunchedEffect(Unit) {
        if (firestoreTags.isEmpty()) {
            viewModel.fetchTagsFromFirestore()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Manga",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BlackWhiteTheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BlackWhiteTheme.iconTint
                        )
                    }
                }

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Cover URL
                    item {
                        OutlinedTextField(
                            value = coverUrl,
                            onValueChange = { coverUrl = it },
                            label = { Text("Cover Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = BlackWhiteTheme.primary,
                                focusedBorderColor = BlackWhiteTheme.primary,
                                unfocusedBorderColor = BlackWhiteTheme.border,
                                focusedLabelColor = BlackWhiteTheme.primary,
                                unfocusedLabelColor = BlackWhiteTheme.border
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Status dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = status,
                                onValueChange = { },
                                label = { Text("Status") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { statusMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Status",
                                            tint = BlackWhiteTheme.iconTint
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    cursorColor = BlackWhiteTheme.primary,
                                    focusedBorderColor = BlackWhiteTheme.primary,
                                    unfocusedBorderColor = BlackWhiteTheme.border,
                                    focusedLabelColor = BlackWhiteTheme.primary,
                                    unfocusedLabelColor = BlackWhiteTheme.border
                                )
                            )

                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                                modifier = Modifier.background(BlackWhiteTheme.surface)
                            ) {
                                statusOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = BlackWhiteTheme.onSurface) },
                                        onClick = {
                                            status = option
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Tags section
                    item {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Selected tags: ${selectedTagIds.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Display tags grouped by category
                    groupedTags.forEach { (group, tagsInGroup) ->
                        item {
                            Text(
                                text = group.capitalize(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BlackWhiteTheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tagsInGroup.forEach { tag ->
                                    val isSelected = selectedTagIds.contains(tag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTagIds = if (isSelected) {
                                                selectedTagIds - tag.id
                                            } else {
                                                selectedTagIds + tag.id
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = tag.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (isSelected) BlackWhiteTheme.primary else BlackWhiteTheme.surface,
                                            labelColor = if (isSelected) BlackWhiteTheme.onPrimary else BlackWhiteTheme.onSurface
                                        ),

                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(BlackWhiteTheme.border)
                        )
                    ) {
                        Text("Cancel")
                    }

                    // Save button
                    Button(
                        onClick = {
                            onSave(title, description, coverUrl, status, selectedTagIds.toList())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        ),
                        enabled = title.isNotEmpty() && coverUrl.isNotEmpty()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}











