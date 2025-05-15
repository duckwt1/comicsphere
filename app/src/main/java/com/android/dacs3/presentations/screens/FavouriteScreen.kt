package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.android.dacs3.presentations.navigation.BottomNavigationBar
import com.android.dacs3.viewmodel.FavouriteViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.setValue
import com.android.dacs3.presentations.components.MangaItem
import com.android.dacs3.utliz.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteScreen(
    navController: NavController,
    viewModel: FavouriteViewModel = hiltViewModel()
) {
    val favourites by viewModel.favourites.observeAsState(emptyList())
    val mangaDetails by viewModel.mangaDetails.observeAsState(emptyList())
    val isRefreshing by viewModel.loading.observeAsState(false)
    val isDeleting by viewModel.isDeleting.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val isVip by viewModel.isVip.observeAsState(false)
    
    // Refresh VIP status when screen is shown
    LaunchedEffect(Unit) {
        viewModel.refreshVipStatus()
    }
    
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showVipLimitInfo by remember { mutableStateOf(false) }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    LaunchedEffect(favourites) {
        viewModel.loadFavouriteDetails()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favourites", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color(0xFF333333)
                ),
                actions = {
                    // VIP Limit Info Button (only for non-VIP users)
                    if (!isVip) {
                        IconButton(onClick = { showVipLimitInfo = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "VIP Info"
                            )
                        }
                    }

                    // Menu dropdown for delete all favourites
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete All Favourites", color = Color(0xFF333333)) },
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
    )
 { paddingValues ->
        // VIP Limit Info Dialog
        if (showVipLimitInfo) {
            AlertDialog(
                onDismissRequest = { showVipLimitInfo = false },
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                confirmButton = {
                    Button(
                        onClick = {
                            showVipLimitInfo = false
                            navController.navigate(Screens.VipScreen.route)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Upgrade to VIP")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showVipLimitInfo = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Close")
                    }
                },
                title = { Text("Favorite Limit Reached") },
                text = {
                    Column {
                        Text("Regular users can save up to 3 favorite mangas.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upgrade to VIP to save unlimited favorites!")

                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = (favourites.size.toFloat() / 3f).coerceAtMost(1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black,
                            trackColor = Color.LightGray
                        )
                        Text(
                            text = "${favourites.size}/3 favorite mangas used",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            )
        }




        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                viewModel.loadFavourites()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                if (error != null) {
                    Text(
                        text = error ?: "Unknown error",
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }

                if (mangaDetails.isEmpty() && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No favourites yet.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(mangaDetails) { manga ->
                            MangaItem(manga = manga, navController = navController)
                        }
                    }
                }
            }
        }
    }
}

