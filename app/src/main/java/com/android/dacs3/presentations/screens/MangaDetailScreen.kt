package com.android.dacs3.presentations.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaDetailUiState
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.FavouriteViewModel
import com.android.dacs3.viewmodel.MangaViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.collectAsState
import com.android.dacs3.presentations.components.CommentSection
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    navController: NavHostController,
    viewModel: MangaViewModel,
    favViewModel: FavouriteViewModel,
    onBackClick: () -> Unit
) {
    val mangaDetail by viewModel.mangaDetail.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val loading by favViewModel.loading.observeAsState(false)
    val error by favViewModel.error.observeAsState()
    val readChapters by viewModel.readChapters.collectAsState()

    // Màu sắc trong theme đen trắng
    val backgroundColor = Color.White
    val textColor = Color.Black
    val cardBackgroundColor = Color(0xFFF5F5F5)
    val dividerColor = Color(0xFFE0E0E0)

    val systemUiController = rememberSystemUiController()

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val isFavourite by favViewModel.isFavourite.observeAsState(false)
    val isVip by favViewModel.isVip.observeAsState(false)
    val maxFavouritesReached by favViewModel.maxFavouritesReached.observeAsState(false)
    
    // Thêm state cho dialog
    var showVipDialog by remember { mutableStateOf(false) }
    
    // Hiển thị dialog khi đạt giới hạn
    LaunchedEffect(maxFavouritesReached) {
        if (maxFavouritesReached) {
            showVipDialog = true
            favViewModel.resetMaxFavouritesReached()
        }
    }
    
    // VIP Dialog
    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = { showVipDialog = false },
            title = {
                Text(
                    text = "Favorite Limit Reached",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    "You have reached the limit of 3 favorite mangas. Upgrade to VIP to enjoy unlimited favorites!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVipDialog = false
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
                    onClick = { showVipDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("Maybe later")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }


    
    val lastReadChapter by viewModel.lastReadChapter.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Cấu hình BottomSheet với trạng thái mặc định là ẩn, nhưng có thể vuốt lên
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    LaunchedEffect(mangaId, selectedLanguage) {
        systemUiController.setSystemBarsColor(backgroundColor, darkIcons = true)
        viewModel.loadMangaDetails(mangaId)
        viewModel.loadChapters(mangaId, selectedLanguage)
        viewModel.loadReadChapters(mangaId, selectedLanguage)

        favViewModel.loadFavourites()
        favViewModel.checkIfFavourite(mangaId)

        // Get the last read chapter
        viewModel.getLastReadChapter(mangaId, selectedLanguage)
        viewModel.loadComments(mangaId)
    }

    // Handle error messages
    LaunchedEffect(error) {
        error?.let {
            // Show error message (you can use a Snackbar or Toast here)
            // After showing the error, clear it
            favViewModel.clearError()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            // Thêm thanh kéo (drag handle) cho BottomSheet
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle(color = textColor.copy(alpha = 0.5f))
            }

            ChaptersBottomSheet(
                chapters = chapters,
                mangaId = mangaId,
                textColor = textColor,
                navController = navController,
                backgroundColor = backgroundColor,
                dividerColor = dividerColor,
                readChapters = readChapters
            )
        },
        sheetPeekHeight = 64.dp, // Hiển thị một phần của BottomSheet
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContainerColor = backgroundColor,
        sheetContentColor = textColor,
        sheetShadowElevation = 8.dp,
        topBar = {
            MangaTopBar(
                onBackClick = onBackClick,
                onAddToFavoriteClick = {
                    if (isFavourite) {
                        favViewModel.removeFavourite(mangaId)
                    } else {
                        favViewModel.addToFavourite(mangaId)
                    }
                },
                textColor = textColor,
                backgroundColor = backgroundColor,
                isFavourite = isFavourite
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    MangaHeader(
                        mangaDetail = mangaDetail,
                        selectedLanguage = selectedLanguage,
                        availableLanguages = availableLanguages,
                        onLanguageSelected = { viewModel.changeLanguage(it) },
                        textColor = textColor,
                        backgroundColor = backgroundColor
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Button(
                        onClick = {
                            if (lastReadChapter != null) {
                                val (chapterId, pageIndex) = lastReadChapter!!
                                navController.navigate(Screens.ChapterScreen.createRoute(mangaId, chapterId, selectedLanguage, pageIndex))
                            } else {
                                val firstChapter = chapters.firstOrNull()
                                if (firstChapter != null) {
                                    navController.navigate(Screens.ChapterScreen.createRoute(mangaId, firstChapter.id, selectedLanguage))
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (lastReadChapter != null) {
                                val (chapterId, pageIndex) = lastReadChapter!!
                                val chapter = chapters.find { it.id == chapterId }
                                val chapterNumber = chapter?.attributes?.chapter ?: "?"
                                "Continue Chapter $chapterNumber - Page $pageIndex"
                            } else {
                                "Start Reading"
                            },
                            color = Color.White
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    MangaDescription(
                        description = mangaDetail.description,
                        isExpanded = isDescriptionExpanded,
                        onExpandToggle = { isDescriptionExpanded = !isDescriptionExpanded },
                        textColor = textColor,
                        cardBackgroundColor = cardBackgroundColor
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    MangaTags(
                        tags = mangaDetail.genres,
                        textColor = textColor,
                        chipBackgroundColor = cardBackgroundColor
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Divider(color = dividerColor)
                }

                item {
                    // Collect comment states
                    val comments by viewModel.comments.collectAsState()
                    val isLoadingComments by viewModel.isLoadingComments.collectAsState()
                    val commentActionInProgress by viewModel.commentActionInProgress.collectAsState()
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    // Comment section
                    CommentSection(
                        comments = comments,
                        currentUserId = currentUserId,
                        isLoading = isLoadingComments || commentActionInProgress,
                        onAddComment = { content -> viewModel.addComment(mangaId, content) },
                        onDeleteComment = { commentId -> viewModel.deleteComment(commentId) }
                    )
                }

                item {
                    Divider(color = dividerColor)
                    // Thêm không gian ở cuối để tránh nội dung bị BottomSheet che khuất
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }

            // Show loading indicator when operation is in progress
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaTopBar(
    onBackClick: () -> Unit,
    onAddToFavoriteClick: () -> Unit,
    textColor: Color,
    backgroundColor: Color,
    isFavourite: Boolean
) {
    TopAppBar(
        title = { Text("", color = textColor) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }
        },
        actions = {
            IconButton(onClick = onAddToFavoriteClick) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavourite) Color.Red else textColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = textColor,
            navigationIconContentColor = textColor,
            actionIconContentColor = textColor
        )
    )
}

@Composable
fun MangaHeader(
    mangaDetail: MangaDetailUiState,
    selectedLanguage: String,
    availableLanguages: List<String>,
    onLanguageSelected: (String) -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = mangaDetail.coverImageUrl,
            contentDescription = mangaDetail.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 120.dp, height = 170.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(mangaDetail.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
            Text("Author: ${mangaDetail.author}", style = MaterialTheme.typography.bodyMedium, color = textColor)
            Text("Status: ${mangaDetail.status}", style = MaterialTheme.typography.bodyMedium, color = textColor)

            Text("Select Language:", fontWeight = FontWeight.Bold, color = textColor)

            LanguageSelector(
                selectedLanguage = selectedLanguage,
                availableLanguages = availableLanguages,
                onLanguageSelected = onLanguageSelected,
                textColor = textColor,
                backgroundColor = backgroundColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    selectedLanguage: String,
    availableLanguages: List<String>,
    onLanguageSelected: (String) -> Unit,
    textColor: Color,
    backgroundColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (availableLanguages.isNotEmpty()) selectedLanguage else "No Language",
            onValueChange = {},
            label = { Text("Language", color = textColor.copy(alpha = 0.7f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = textColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = textColor
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(backgroundColor)
        ) {
            availableLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language, color = textColor) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MangaDescription(
    description: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    textColor: Color,
    cardBackgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textColor
                    )
                }
            }

            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                color = textColor
            )
        }
    }
}

@Composable
fun MangaTags(
    tags: List<String>,
    textColor: Color,
    chipBackgroundColor: Color
) {
    Text("Tags:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = {},
                label = { Text(tag, fontSize = 12.sp, color = textColor) },
                shape = RoundedCornerShape(16.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipBackgroundColor,
                    labelColor = textColor
                )
            )
        }
    }
}

@Composable
fun ChaptersBottomSheet(
    chapters: List<ChapterData>,
    mangaId: String,
    textColor: Color,
    navController: NavHostController,
    backgroundColor: Color,
    dividerColor: Color,
    readChapters: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val sortedChapters = chapters.sortedByDescending { it.attributes.chapter?.toFloatOrNull() ?: 0f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "${chapters.size} chapters",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        Divider(color = dividerColor)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(sortedChapters) { chapter ->
                ChapterItem(
                    chapter = chapter,
                    textColor = textColor,
                    dividerColor = dividerColor,
                    isRead = chapter.id in readChapters,
                    onClick = {
                        val externalUrl = chapter.attributes.externalUrl
                        if (externalUrl != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl))
                            context.startActivity(intent)
                        } else {
                            navController.navigate(Screens.ChapterScreen.createRoute(
                                mangaId,
                                chapter.id,
                                chapter.attributes.translatedLanguage
                            ))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChapterItem(
    chapter: ChapterData,
    textColor: Color,
    dividerColor: Color,
    isRead: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Chapter ${chapter.attributes.chapter ?: "?"}",
            style = MaterialTheme.typography.titleMedium,
            color = if (isRead) textColor.copy(alpha = 0.5f) else textColor
        )

        if (!chapter.attributes.title.isNullOrEmpty()) {
            Text(
                text = chapter.attributes.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRead) textColor.copy(alpha = 0.4f) else textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = dividerColor
        )
    }
}


