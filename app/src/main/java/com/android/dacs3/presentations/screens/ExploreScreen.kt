package com.android.dacs3.presentations.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.dacs3.R
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.TagWrapper
import com.android.dacs3.presentations.navigation.BottomNavigationBar
import com.android.dacs3.viewmodel.MangaViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: MangaViewModel = hiltViewModel()
) {
    val mangas by viewModel.mangas.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val tags by viewModel.tags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("All Manga", "For You", "Trending")

    var showTagFilter by remember { mutableStateOf(false) }

    // Initialize data when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadReadingProgress()
        viewModel.fetchTags()
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color(0xFFF8F8F8),
                        modifier = Modifier.weight(1f)
                    ) {
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

                    IconButton(onClick = { showTagFilter = !showTagFilter }) {
                        val iconResId = if (showTagFilter) R.drawable.ic_filter_2 else R.drawable.ic_filter_1
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = if (showTagFilter) "Close Filter" else "Open Filter",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }

                }

                // Tag filter section
                AnimatedVisibility(visible = showTagFilter) {
                    TagFilterSection(
                        tags = tags,
                        selectedTags = selectedTags,
                        onTagSelected = { tagId, selected ->
                            viewModel.updateSelectedTags(tagId, selected)
                        },
                        onApplyFilter = {
                            viewModel.applyTagFilter()
                            showTagFilter = false
                        },
                        onClearAllTags = {viewModel.clearAllTags()}
                    )
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

        // Show Load More button if:
        // 1. There are items
        // 2. We have at least 3 items (to maintain grid layout)
        // 3. If it's search results, only show if we have 21 or more items
        if (mangas.isNotEmpty() && mangas.size >= 3 && 
            ( mangas.size >= 21)) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterSection(
    tags: List<TagWrapper>,
    selectedTags: List<String>,
    onTagSelected: (String, Boolean) -> Unit,
    onApplyFilter: () -> Unit,
    onClearAllTags: () -> Unit
) {
    // Group tags
    val groupedTags = tags.groupBy { it.attributes.group }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(12.dp)
        ) {
            Text(
                text = "Filter by Tags",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (tags.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .fillMaxWidth()
                ) {
                    // display tag in group
                    groupedTags.forEach { (group, tagsInGroup) ->
                        item {
                            // Touppercase the first letter
                            val formattedGroupName = group.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }

                            Text(
                                text = formattedGroupName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            FlowRow(
                                mainAxisSpacing = 6.dp,
//                                crossAxisSpacing = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tagsInGroup.forEach { tag ->
                                    val isSelected = selectedTags.contains(tag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onTagSelected(tag.id, !isSelected) },
                                        label = {
                                            Text(
                                                text = tag.attributes.name["en"] ?: "Unknown",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        }, colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.LightGray, // Background color when selected
                                            selectedLabelColor = Color.Black,           // Color when selected
                                            containerColor = Color.White,          // Background color when not selected
                                            labelColor = Color.DarkGray                 // Color when not selected
                                        )
                                    )
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.LightGray,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onClearAllTags // Clear when click
                    ,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray
                    )
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = onApplyFilter,
                    shape = MaterialTheme.shapes.medium,

                    ) {
                    Text("Apply Filter")
                }
            }
        }
    }
}