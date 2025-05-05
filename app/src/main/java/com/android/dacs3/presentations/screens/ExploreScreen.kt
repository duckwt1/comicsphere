package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.presentations.navigation.BottomNavigationBar
import com.android.dacs3.viewmodel.MangaViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: MangaViewModel = hiltViewModel()
) {
    val mangas by viewModel.mangas.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("All Manga", "For You", "Trending")

    // Initialize recommendations when screen is first displayed
    LaunchedEffect(Unit) {
        // Pre-load reading progress for recommendations
        viewModel.loadReadingProgress()
    }

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    modifier = Modifier.padding(8.dp),
                    onSearch = { query ->
                        searchQuery = query
                        if (query.isBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.fetchMangaList(reset = true)
                                1 -> viewModel.fetchRecommendedManga(reset = true)
                                2 -> viewModel.fetchTrendingManga(reset = true)
                            }
                        } else {
                            viewModel.searchManga(query)
                        }
                    }
                )

                TabRow(selectedTabIndex = selectedTabIndex,containerColor = Color(0xFFF8F8F8)) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                searchQuery = ""
                                when (index) {
                                    0 -> viewModel.fetchMangaList(reset = true)
                                    1 -> viewModel.fetchRecommendedManga(reset = true)
                                    2 -> viewModel.fetchTrendingManga(reset = true)
                                }
                            },
                            text = { Text(title, color = if (selectedTabIndex == index) Color.Gray else Color.Black) }
                        )
                    }
                }
            }
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (searchQuery.isBlank()) {
                    when (selectedTabIndex) {
                        0 -> viewModel.refreshMangaList()
                        1 -> viewModel.fetchRecommendedManga(reset = true)
                        2 -> viewModel.fetchTrendingManga(reset = true)
                    }
                } else {
                    viewModel.searchManga(searchQuery)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                MangaList(
                    mangas = mangas,
                    navController = navController,
                    onLoadMore = {
                        if (searchQuery.isBlank()) {
                            when (selectedTabIndex) {
                                0 -> viewModel.fetchMangaList() // All Manga
                                1 -> viewModel.fetchRecommendedManga(reset = false) // For You
                                2 -> viewModel.fetchTrendingManga(reset = false) // Trending Manga
                            }
                        }
                    }
                )

            }
        }
    }
}

@Composable
fun MangaList(
    mangas: List<MangaData>,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(mangas) { manga ->
            MangaItem(manga = manga, navController = navController)
        }

        item {
            Spacer(modifier = Modifier)
        }
        item {
            Button(
                onClick = { onLoadMore() },
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .background(Color.Transparent)
            ) {
                Text(text = "Load More", fontSize = 12.sp, maxLines = 1)
            }
        }
        item {
            Spacer(modifier = Modifier)
        }
    }
}
