package com.android.dacs3.presentations.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
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

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Debug logs for state changes
    LaunchedEffect(readingProgress, mangaDetails, chapterDetails) {
        Log.d("HistoryScreen", "State changed - ReadingProgress: ${readingProgress.size}, MangaDetails: ${mangaDetails.size}, ChapterDetails: ${chapterDetails.size}")
    }

    val latestProgressByManga = readingProgress
        .groupBy { it.mangaId }
        .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }!! }
        .values
        .sortedByDescending { it.timestamp }

    // Debug log for latest progress
    LaunchedEffect(latestProgressByManga) {
        Log.d("HistoryScreen", "Latest progress items: ${latestProgressByManga.size}")
        latestProgressByManga.forEach { progress ->
            val hasManga = mangaDetails.containsKey(progress.mangaId)
            val hasChapter = chapterDetails.containsKey(progress.chapterId)
            Log.d("HistoryScreen", "Progress ${progress.mangaId}: hasManga=$hasManga, hasChapter=$hasChapter")
        }
    }

    val groupedHistory = latestProgressByManga
        .groupBy { progress -> viewModel.formatHistoryDate(progress.timestamp) }

    // Debug log for grouped history
    LaunchedEffect(groupedHistory) {
        Log.d("HistoryScreen", "Grouped history items: ${groupedHistory.size}")
        groupedHistory.forEach { (date, progresses) ->
            Log.d("HistoryScreen", "Date $date: ${progresses.size} items")
        }
    }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            viewModel.loadReadingProgress()
            // Don't set isLoading to false here, let it be controlled by the data loading state
        } catch (e: Exception) {
            error = e.message ?: "An error occurred"
            isLoading = false
        }
    }

    // Update loading state based on data availability
    LaunchedEffect(readingProgress, mangaDetails, chapterDetails) {
        val hasData = readingProgress.isNotEmpty() && 
                     mangaDetails.isNotEmpty() && 
                     chapterDetails.isNotEmpty()
        isLoading = !hasData
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F8F8),
                    titleContentColor = Color.Black
                )
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
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
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
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                latestProgressByManga.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Empty History",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No reading history found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        groupedHistory.forEach { (date, progresses) ->
                            item {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF666666)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }

                            items(progresses) { progress ->
                                val manga = mangaDetails[progress.mangaId]
                                val chapter = chapterDetails[progress.chapterId]

                                if (manga != null && chapter != null) {
                                    val title = manga.attributes.title["en"] ?: manga.attributes.title.values.firstOrNull() ?: "Unknown"
                                    val chapterNumber = chapter.attributes.chapter ?: ""
                                    val chapterTitle = chapter.attributes.title ?: ""
                                    val imageUrl = manga.coverImageUrl ?: ""

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

@Composable
fun HistoryItem(
    title: String,
    chapterNumber: String,
    chapterTitle: String,
    imageUrl: String,
    timestamp: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chapter $chapterNumber${if (chapterTitle.isNotEmpty()) ": $chapterTitle" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFF999999)
                )
            }
        }
    }
}

