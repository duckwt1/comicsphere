package com.android.dacs3.presentations.screens

import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.viewmodel.MangaViewModel
import com.example.financial_app.presentation.navigation.BottomNavigationBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: MangaViewModel = hiltViewModel()
) {
    val mangas by viewModel.mangas.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = Color.Transparent, darkIcons = true)

    // Trạng thái tìm kiếm
    var searchQuery by remember { mutableStateOf("") }
    val filteredMangas = if (searchQuery.isEmpty()) {
        mangas
    } else {
        mangas.filter { manga ->
            manga.attributes.title["en"]?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            SearchBar(
                modifier = Modifier.padding(8.dp),
                onSearch = { query -> searchQuery = query }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshMangaList() },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                MangaList(
                    mangas = filteredMangas,
                    navController = navController,
                    onLoadMore = { viewModel.fetchMangaList() }
                )
            }
        }
    }
}

@Composable
fun MangaList(mangas: List<MangaData>, navController: NavController, onLoadMore: () -> Unit) {
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

        // Detect scroll to the end of the list
        item {
            Spacer(modifier = Modifier.height(8.dp))
            onLoadMore()
        }
    }
}