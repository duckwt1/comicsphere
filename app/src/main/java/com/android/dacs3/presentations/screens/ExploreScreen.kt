package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.viewmodel.MangaViewModel
import com.example.financial_app.presentation.navigation.BottomNavigationBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: MangaViewModel = hiltViewModel()
) {
    val mangas by viewModel.mangas.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SearchBar(
                modifier = Modifier.padding(8.dp),
                onSearch = { query ->
                    searchQuery = query
                    if (query.isBlank()) {
                        viewModel.fetchMangaList(reset = true)
                    } else {
                        viewModel.searchManga(query)
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (searchQuery.isBlank()) {
                    viewModel.refreshMangaList()
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
                            viewModel.fetchMangaList()
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
            androidx.compose.material3.Button(
                onClick = { onLoadMore() },
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .background(Color.Transparent)
//                    .padding(8.dp)
            ) {
                Text(text = "Load More", fontSize = 12.sp, maxLines = 1)
            }
        }
        item {
            Spacer(modifier = Modifier)
        }
    }
}
