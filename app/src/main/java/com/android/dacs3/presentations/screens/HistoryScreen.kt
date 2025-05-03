package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    // Group progress by formatted date with Today/Yesterday logic
    val groupedHistory = readingProgress
        .distinctBy { it.mangaId }
        .groupBy { progress ->
            viewModel.formatHistoryDate(progress.timestamp)
        }


    // Load manga details
    LaunchedEffect(readingProgress) {
        viewModel.loadReadingProgress()
    }

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("History") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = Color.White
//                )
//            )
//        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
        ) {
            groupedHistory.forEach { (date, progresses) ->
                item {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        color = Color.Black
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp, max = 500.dp), // prevents nested scroll conflict
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false, // disable scroll to allow LazyColumn to handle it
                        content = {
                            items(progresses) { progress ->
                                val manga = mangaDetails[progress.mangaId]
                                if (manga != null) {
                                    MangaItem(
                                        manga = manga,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
