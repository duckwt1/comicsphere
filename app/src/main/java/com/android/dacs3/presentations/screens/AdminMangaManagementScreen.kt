package com.android.dacs3.presentations.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.ChapterAttributes
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.viewmodel.AdminViewModel
import com.google.firebase.firestore.Query
import java.util.UUID
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    
    // Add this LaunchedEffect to load manga data when the screen is first composed
    LaunchedEffect(key1 = Unit) {
        // Ensure manga list is loaded when screen opens
        viewModel.loadMangaList()
    }

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
            onDismiss = { 
                showMangaDetailsDialog = false
                viewModel.resetUploadState()
            },
            onDeleteManga = {
                showMangaDetailsDialog = false
                showDeleteConfirmDialog = true
            },
            onEditManga = {
                showEditMangaDialog = true
            },
            viewModel = viewModel
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
            onDismiss = { 
                showAddMangaDialog = false
                viewModel.resetUploadState()
            },
            onAdd = { title, description, coverUrl, status, author, tags ->
                viewModel.addManga(title, description, coverUrl, status, author, tags)
                showAddMangaDialog = false
                viewModel.resetUploadState()
            }
        )
    }

    // Edit manga dialog
    if (showEditMangaDialog && selectedManga != null) {
        EditMangaDialog(
            manga = selectedManga!!,
            onDismiss = { 
                showEditMangaDialog = false
                viewModel.resetUploadState()
            },
            onSave = { title, description, coverUrl, status, author, tags ->
                viewModel.updateManga(selectedManga!!.id, title, description, coverUrl, status, author, tags)
                showEditMangaDialog = false
                showMangaDetailsDialog = false
                viewModel.resetUploadState()
            }
        )
    }
}

@Composable
fun MangaListItem(
    manga: MangaData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlackWhiteTheme.surface
        ),
        border = BorderStroke(1.dp, BlackWhiteTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName)
                    .crossfade(true)
                    .build(),
                contentDescription = "Manga cover",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = manga.attributes.title["en"] ?: "Unknown title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Author: ${manga.attributes.author ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Status: ${manga.attributes.status ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "View details",
                tint = BlackWhiteTheme.iconTint
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MangaDetailsDialog(
    manga: MangaData,
    onDismiss: () -> Unit,
    onDeleteManga: () -> Unit,
    onEditManga: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var showAddChapterDialog by remember { mutableStateOf(false) }
    
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

                                    Text(
                                        text = "Author: ${manga.attributes.author ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BlackWhiteTheme.onSurface
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

                        item {
                            Divider(color = BlackWhiteTheme.divider, modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "Chapters",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackWhiteTheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            // Load chapters when dialog opens
                            LaunchedEffect(manga.id) {
                                viewModel.loadChaptersForManga(manga.id)
                            }
                            
                            val chapters by viewModel.mangaChapters.collectAsState()
                            
                            if (chapters.isEmpty()) {
                                Text(
                                    text = "No chapters available",
                                    color = BlackWhiteTheme.divider,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                chapters.forEach { chapter ->
                                    ChapterListItem(
                                        chapter = chapter,
                                        onDelete = {
                                            viewModel.deleteChapter(manga.id, chapter.id)
                                        }
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { 
                                    showAddChapterDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlackWhiteTheme.primary,
                                    contentColor = BlackWhiteTheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add New Chapter")
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

    if (showAddChapterDialog) {
        AddChapterDialog(
            mangaId = manga.id,
            onDismiss = { showAddChapterDialog = false },
            onAdd = { chapterNumber, title, language, imageUrls ->
                viewModel.addChapter(manga.id, chapterNumber, title, language, imageUrls)
                showAddChapterDialog = false
            }
        )
    }
}

@Composable
fun ChapterListItem(
    chapter: ChapterData,
    onDelete: () -> Unit
) {
    val chapterNumber = chapter.attributes.chapter ?: "N/A"
    val title = chapter.attributes.title ?: "Untitled"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlackWhiteTheme.surface
        ),
        border = BorderStroke(1.dp, BlackWhiteTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Chapter $chapterNumber",
                    fontWeight = FontWeight.Bold,
                    color = BlackWhiteTheme.onSurface
                )
                Text(
                    text = title,
                    color = BlackWhiteTheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Chapter",
                    tint = BlackWhiteTheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterDialog(
    mangaId: String,
    onDismiss: () -> Unit,
    onAdd: (chapterNumber: String, title: String, language: String, imageUrls: List<String>) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var chapterNumber by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var imageUrlInput by remember { mutableStateOf("") }
    var imageUrls by remember { mutableStateOf(listOf<String>()) }
    
    // For image upload
    val isUploading by viewModel.isUploading.collectAsState()
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Launch coroutine to handle the upload
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                viewModel.uploadChapterImage(it).collect { result ->
                    when (result) {
                        is AdminViewModel.Result.Success -> {
                            // Add the uploaded URL to the list
                            imageUrls = imageUrls + result.data
                            Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                        is AdminViewModel.Result.Failure -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                        is AdminViewModel.Result.Loading -> {
                            // Could update a progress indicator here
                        }
                    }
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Add New Chapter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackWhiteTheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Chapter number
                OutlinedTextField(
                    value = chapterNumber,
                    onValueChange = { chapterNumber = it },
                    label = { Text("Chapter Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = BlackWhiteTheme.primary,
                        focusedBorderColor = BlackWhiteTheme.primary,
                        unfocusedBorderColor = BlackWhiteTheme.border
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Chapter title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = BlackWhiteTheme.primary,
                        focusedBorderColor = BlackWhiteTheme.primary,
                        unfocusedBorderColor = BlackWhiteTheme.border
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Language dropdown
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (language) {
                            "en" -> "English"
                            "vi" -> "Vietnamese"
                            else -> "English"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Language") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = BlackWhiteTheme.primary,
                            focusedBorderColor = BlackWhiteTheme.primary,
                            unfocusedBorderColor = BlackWhiteTheme.border
                        )
                    )
                    
                    // Dropdown menu content
                    DropdownMenu(
                        expanded = false,
                        onDismissRequest = { },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = { language = "en" }
                        )
                        DropdownMenuItem(
                            text = { Text("Vietnamese") },
                            onClick = { language = "vi" }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image upload section
                Text(
                    text = "Chapter Images",
                    fontWeight = FontWeight.Medium,
                    color = BlackWhiteTheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Upload button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.primary,
                        contentColor = BlackWhiteTheme.onPrimary
                    ),
                    enabled = !isUploading
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUploading) "Uploading..." else "Upload Image")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Manual URL input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("Image URL") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            cursorColor = BlackWhiteTheme.primary,
                            focusedBorderColor = BlackWhiteTheme.primary,
                            unfocusedBorderColor = BlackWhiteTheme.border
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            if (imageUrlInput.isNotEmpty()) {
                                imageUrls = imageUrls + imageUrlInput
                                imageUrlInput = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Image URL",
                            tint = BlackWhiteTheme.primary
                        )
                    }
                }
                
                // Display added image URLs
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        items(imageUrls) { url ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Image preview thumbnail
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Image preview",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // URL text
                                Text(
                                    text = url,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // Remove button
                                IconButton(
                                    onClick = {
                                        imageUrls = imageUrls.filter { it != url }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove URL",
                                        tint = BlackWhiteTheme.error
                                    )
                                }
                            }
                            
                            Divider(
                                color = BlackWhiteTheme.divider,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Show loading indicator when uploading
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(BlackWhiteTheme.surface.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = BlackWhiteTheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Uploading image...", color = BlackWhiteTheme.primary)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(BlackWhiteTheme.border)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onAdd(chapterNumber, title, language, imageUrls)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        ),
                        enabled = chapterNumber.isNotEmpty() && imageUrls.isNotEmpty() && !isUploading
                    ) {
                        Text("Add Chapter")
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
    onAdd: (title: String, description: String, coverUrl: String, status: String, author: String, tagIds: List<String>) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ongoing") }
    var author by remember { mutableStateOf("") }
    
    // For image upload
    var showImagePicker by remember { mutableStateOf(false) }
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Launch coroutine to handle the upload
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                viewModel.uploadMangaCover(it).collect { result ->
                    when (result) {
                        is AdminViewModel.Result.Success -> {
                            coverUrl = result.data
                            Toast.makeText(context, "Cover uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                        is AdminViewModel.Result.Failure -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                        is AdminViewModel.Result.Loading -> {
                            // Could update a progress indicator here
                        }
                    }
                }
            }
        }
    }
    
    // Update coverUrl when uploadedImageUrl changes
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let {
            coverUrl = it
        }
    }
    
    // Selected tagIds
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
    
    // Get tags from viewModel
    val tags by viewModel.firestoreTags.collectAsState()
    
    // Reset upload state when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUploadState()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Add New Manga",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackWhiteTheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Title field
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
                    
                    // Description field
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
                    
                    // Cover image section
                    item {
                        Text(
                            text = "Cover Image",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover image preview
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 150.dp)
                                    .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BlackWhiteTheme.background)
                            ) {
                                if (coverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "No image",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.Center),
                                        tint = BlackWhiteTheme.divider
                                    )
                                }
                                
                                // Show loading indicator when uploading
                                if (isUploading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BlackWhiteTheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Upload button
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlackWhiteTheme.primary,
                                        contentColor = BlackWhiteTheme.onPrimary
                                    ),
                                    enabled = !isUploading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Cover")
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // URL input field
                                OutlinedTextField(
                                    value = coverUrl,
                                    onValueChange = { coverUrl = it },
                                    label = { Text("Cover URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        cursorColor = BlackWhiteTheme.primary,
                                        focusedBorderColor = BlackWhiteTheme.primary,
                                        unfocusedBorderColor = BlackWhiteTheme.border,
                                        focusedLabelColor = BlackWhiteTheme.primary,
                                        unfocusedLabelColor = BlackWhiteTheme.border
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Status dropdown
                    item {
                        Text(
                            text = "Status",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Status options
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusOption(
                                text = "Ongoing",
                                selected = status == "ongoing",
                                onClick = { status = "ongoing" }
                            )
                            
                            StatusOption(
                                text = "Completed",
                                selected = status == "completed",
                                onClick = { status = "completed" }
                            )
                            
                            StatusOption(
                                text = "Hiatus",
                                selected = status == "hiatus",
                                onClick = { status = "hiatus" }
                            )
                            
                            StatusOption(
                                text = "Cancelled",
                                selected = status == "cancelled",
                                onClick = { status = "cancelled" }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Author field
                    item {
                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Author(s)") },
                            placeholder = { Text("Separate multiple authors with commas") },
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
                    
                    // Tags section
                    item {
                        Text(
                            text = "Tags",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Group tags by category
                        val groupedTags = tags.groupBy { it.group }
                        
                        groupedTags.forEach { (group, tagsInGroup) ->
                            Text(
                                text = group.capitalize(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        label = { Text(tag.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BlackWhiteTheme.primary,
                                            selectedLabelColor = BlackWhiteTheme.onPrimary
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetUploadState() // Reset upload state when canceling
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(BlackWhiteTheme.border)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Add button
                    Button(
                        onClick = {
                            onAdd(title, description, coverUrl, status, author, selectedTagIds.toList())
                            viewModel.resetUploadState() // Reset upload state after adding
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        ),
                        enabled = title.isNotEmpty() && coverUrl.isNotEmpty() && !isUploading
                    ) {
                        Text("Add Manga")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) BlackWhiteTheme.primary else BlackWhiteTheme.surface,
        border = BorderStroke(1.dp, if (selected) BlackWhiteTheme.primary else BlackWhiteTheme.border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) BlackWhiteTheme.onPrimary else BlackWhiteTheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMangaDialog(
    manga: MangaData,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, coverUrl: String, status: String, author: String, tagIds: List<String>) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val initialTitle = manga.attributes.title["en"] ?: ""
    val initialDescription = manga.attributes.description["en"] ?: ""
    val initialCoverUrl = manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName ?: ""
    val initialStatus = manga.attributes.status ?: "ongoing"
    val initialAuthor = manga.attributes.author ?: ""
    
    // Lấy danh sách tagIds từ manga
    val initialTagIds = manga.attributes.tags.map { it.id }

    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var coverUrl by remember { mutableStateOf(initialCoverUrl) }
    var status by remember { mutableStateOf(initialStatus) }
    var author by remember { mutableStateOf(initialAuthor) }
    
    // For image upload
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadedImageUrl by viewModel.uploadedImageUrl.collectAsState()
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Launch coroutine to handle the upload
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                viewModel.uploadMangaCover(it).collect { result ->
                    when (result) {
                        is AdminViewModel.Result.Success -> {
                            coverUrl = result.data
                            Toast.makeText(context, "Cover uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                        is AdminViewModel.Result.Failure -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                        is AdminViewModel.Result.Loading -> {
                            // Could update a progress indicator here
                        }
                    }
                }
            }
        }
    }
    
    // Update coverUrl when uploadedImageUrl changes
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let {
            coverUrl = it
        }
    }
    
    // Selected tagIds
    var selectedTagIds by remember { mutableStateOf(initialTagIds.toSet()) }
    
    // Get tags from viewModel
    val tags by viewModel.firestoreTags.collectAsState()
    
    // Reset upload state when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUploadState()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = BlackWhiteTheme.surface,
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Edit Manga",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackWhiteTheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Title field
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
                    
                    // Description field
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
                    
                    // Cover image section
                    item {
                        Text(
                            text = "Cover Image",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover image preview
                            Box(
                                modifier = Modifier
                                    .size(100.dp, 150.dp)
                                    .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BlackWhiteTheme.background)
                            ) {
                                if (coverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "No image",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.Center),
                                        tint = BlackWhiteTheme.divider
                                    )
                                }
                                
                                // Show loading indicator when uploading
                                if (isUploading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BlackWhiteTheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Upload button
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlackWhiteTheme.primary,
                                        contentColor = BlackWhiteTheme.onPrimary
                                    ),
                                    enabled = !isUploading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Cover")
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // URL input field
                                OutlinedTextField(
                                    value = coverUrl,
                                    onValueChange = { coverUrl = it },
                                    label = { Text("Cover URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        cursorColor = BlackWhiteTheme.primary,
                                        focusedBorderColor = BlackWhiteTheme.primary,
                                        unfocusedBorderColor = BlackWhiteTheme.border,
                                        focusedLabelColor = BlackWhiteTheme.primary,
                                        unfocusedLabelColor = BlackWhiteTheme.border
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Status dropdown
                    item {
                        Text(
                            text = "Status",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Status options
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusOption(
                                text = "Ongoing",
                                selected = status == "ongoing",
                                onClick = { status = "ongoing" }
                            )
                            
                            StatusOption(
                                text = "Completed",
                                selected = status == "completed",
                                onClick = { status = "completed" }
                            )
                            
                            StatusOption(
                                text = "Hiatus",
                                selected = status == "hiatus",
                                onClick = { status = "hiatus" }
                            )
                            
                            StatusOption(
                                text = "Cancelled",
                                selected = status == "cancelled",
                                onClick = { status = "cancelled" }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Author field
                    item {
                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Author(s)") },
                            placeholder = { Text("Separate multiple authors with commas") },
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
                    
                    // Tags section
                    item {
                        Text(
                            text = "Tags",
                            fontWeight = FontWeight.Medium,
                            color = BlackWhiteTheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Group tags by category
                        val groupedTags = tags.groupBy { it.group }
                        
                        groupedTags.forEach { (group, tagsInGroup) ->
                            Text(
                                text = group.capitalize(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = BlackWhiteTheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        label = { Text(tag.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BlackWhiteTheme.primary,
                                            selectedLabelColor = BlackWhiteTheme.onPrimary
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetUploadState() // Reset upload state when canceling
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(BlackWhiteTheme.border)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Save button
                    Button(
                        onClick = {
                            onSave(title, description, coverUrl, status, author, selectedTagIds.toList())
                            viewModel.resetUploadState() // Reset upload state after saving
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        ),
                        enabled = title.isNotEmpty() && coverUrl.isNotEmpty() && !isUploading
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}



























