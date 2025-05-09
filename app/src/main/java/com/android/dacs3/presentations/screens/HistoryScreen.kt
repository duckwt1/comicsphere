package com.android.dacs3.presentations.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.android.dacs3.R
import com.android.dacs3.data.model.ReadingProgress
import com.android.dacs3.data.model.coverImageUrl
import com.android.dacs3.presentations.navigation.BottomNavigationBar
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.MangaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: MangaViewModel
) {
    val readingProgress by viewModel.readingProgress.collectAsState()
    val mangaDetails by viewModel.mangaDetails.collectAsState()
    val chapterDetails by viewModel.chapterDetails.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var selectedMangaId by remember { mutableStateOf<String?>(null) }
    var showDeleteMangaDialog by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    // Show error dialog if there's a deletion error
    deleteError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel._deleteError.value = null },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel._deleteError.value = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Lấy dữ liệu lịch sử đọc mới nhất cho mỗi manga
    val latestProgressByManga = readingProgress
        .groupBy { it.mangaId }
        .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }!! }
        .values
        .sortedByDescending { it.timestamp }

    // Nhóm lịch sử đọc theo ngày
    val groupedHistory = latestProgressByManga
        .groupBy { progress -> viewModel.formatHistoryDate(progress.timestamp) }

    // Load dữ liệu khi vào màn hình
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            viewModel.loadReadingProgress()
        } catch (e: Exception) {
            error = e.message ?: "An error occurred"
            isLoading = false
        }
    }

    // Cập nhật trạng thái loading dựa trên việc tải dữ liệu
    LaunchedEffect(readingProgress, mangaDetails, chapterDetails) {
        val hasData = readingProgress.isNotEmpty() &&
                mangaDetails.isNotEmpty() &&
                chapterDetails.isNotEmpty()
        isLoading = !hasData && !isDeleting
    }

    // Dialog xác nhận xoá tất cả lịch sử
    if (showDeleteAllDialog) {
        DeleteConfirmationDialog(
            title = "Delete All History",
            message = "Are you sure you want to delete your entire reading history? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteAllMangaReadingProgress()
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    // Dialog xác nhận xoá lịch sử của một manga
    if (showDeleteMangaDialog && selectedMangaId != null) {
        val mangaTitle = mangaDetails[selectedMangaId]?.attributes?.title?.get("en")
            ?: mangaDetails[selectedMangaId]?.attributes?.title?.values?.firstOrNull()
            ?: "this manga"

        DeleteConfirmationDialog(
            title = "Delete Manga History",
            message = "Are you sure you want to delete the reading history of \"$mangaTitle\"? This action cannot be undone.",
            onConfirm = {
                selectedMangaId?.let { mangaId ->
                    // Get all chapters for this manga
                    val mangaChapters = readingProgress.filter { it.mangaId == mangaId }
                    mangaChapters.forEach { progress ->
                        viewModel.deleteReadingProgress(
                            mangaId = progress.mangaId,
                            chapterId = progress.chapterId,
                            language = progress.language
                        )
                    }
                }
                showDeleteMangaDialog = false
                selectedMangaId = null
            },
            onDismiss = {
                showDeleteMangaDialog = false
                selectedMangaId = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_history),
                            contentDescription = null,
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reading History", color = Color(0xFF333333))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F8F8),
                    titleContentColor = Color.Black
                ),
                actions = {
                    // Menu dropdown for delete all history
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color(0xFF333333)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete All History", color = Color(0xFF333333)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete All",
                                        tint = Color(0xFF333333)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    showDeleteAllDialog = true
                                }
                            )
                        }
                    }

                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    ErrorDisplay(errorMessage = error!!)
                }
                latestProgressByManga.isEmpty() -> {
                    EmptyHistoryDisplay()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedHistory.forEach { (date, progresses) ->
                            item {
                                DateHeader(date = date)
                            }

                            items(progresses) { progress ->
                                val manga = mangaDetails[progress.mangaId]
                                val chapter = chapterDetails[progress.chapterId]

                                if (manga != null && chapter != null) {
                                    val title = manga.attributes.title["en"] ?: manga.attributes.title.values.firstOrNull() ?: "Unknown"
                                    val chapterNumber = chapter.attributes.chapter ?: ""
                                    val chapterTitle = chapter.attributes.title ?: ""
                                    val imageUrl = manga.coverImageUrl ?: ""

                                    key(progress.mangaId + progress.chapterId) {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(300)) +
                                                    slideInHorizontally(animationSpec = tween(300)) { it },
                                            exit = fadeOut(animationSpec = tween(300)) +
                                                    slideOutHorizontally(animationSpec = tween(300)) { it }
                                        ) {
                                            HistoryItem(
                                                title = title,
                                                chapterNumber = chapterNumber,
                                                chapterTitle = chapterTitle,
                                                imageUrl = imageUrl,
                                                timestamp = progress.timestamp,
                                                onClick = {
                                                    navController.navigate(Screens.DetailsScreen.createRoute(manga.id))
                                                },
                                                onDelete = {
                                                    viewModel.deleteReadingProgress(
                                                        mangaId = progress.mangaId,
                                                        chapterId = progress.chapterId,
                                                        language = progress.language
                                                    )
                                                },
                                                onDeleteAllManga = {
                                                    selectedMangaId = progress.mangaId
                                                    showDeleteMangaDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier
                .weight(0.2f)
                .height(1.dp),
            color = Color(0xFFDDDDDD)
        )

        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666)
            ),
            modifier = Modifier
                .weight(0.6f)
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )

        Divider(
            modifier = Modifier
                .weight(0.2f)
                .height(1.dp),
            color = Color(0xFFDDDDDD)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(
    title: String,
    chapterNumber: String,
    chapterTitle: String,
    imageUrl: String,
    timestamp: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDeleteAllManga: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x1A000000)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp, 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shadow(1.dp, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Chapter $chapterNumber${if (chapterTitle.isNotEmpty()) ": $chapterTitle" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Read time with better format
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Read at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
            }

            // Delete options
            Box {
                IconButton(
                    onClick = { showOptions = !showOptions }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF999999)
                    )
                }

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete this chapter") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                        onClick = {
                            showOptions = false
                            onDelete()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Delete all history of this manga") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete All") },
                        onClick = {
                            showOptions = false
                            onDeleteAllManga()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorDisplay(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun EmptyHistoryDisplay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Empty History",
                tint = Color.Gray,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No reading history yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start reading manga and your history will appear here",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = Color.Black)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Text("Delete", color = Color.White)
                    }
                }
            }
        }
    }
}
