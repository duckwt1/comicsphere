package com.android.dacs3.presentations.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.android.dacs3.viewmodel.MangaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ReadingMode {
    VERTICAL_SCROLL, HORIZONTAL_SWIPE, CONTINUOUS_SCROLL
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    mangaId: String,
    chapterId: String,
    language: String,
    pageIndex: Int = 0,
    navController: NavHostController,
    viewModel: MangaViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val chapterImageUrls by viewModel.chapterImageUrls.collectAsState()
    val currentPage by viewModel.currentPageReading.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val chapterTitle by viewModel.chapterTitle.collectAsState()
    
    // Thêm state để theo dõi khi chapter thay đổi
    var currentChapterId by remember { mutableStateOf(chapterId) }
    var initialPageIndex by remember { mutableStateOf(pageIndex) }
    
    // Khởi tạo listState và pagerState với initialPageIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { chapterImageUrls.size })
    val coroutineScope = rememberCoroutineScope()

    var readingMode by remember { mutableStateOf(ReadingMode.HORIZONTAL_SWIPE) } // Default to horizontal

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val minScale = 1f
    val maxScale = 3f

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 300),
        label = "scaleAnimation"
    )

    val pagePositions = remember { mutableStateMapOf<Int, Offset>() }
    val isZooming by remember { derivedStateOf { scale > 1.05f } }
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }

    // Collect states from ViewModel
    val showControls by viewModel.showControls.collectAsState()
    val showNextChapterButton by viewModel.showNextChapterButton.collectAsState()
    val isLoadingNextChapter by viewModel.isLoadingNextChapter.collectAsState()

    // Show/hide controls with timeout
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            viewModel.hideControls()
        }
    }

    // Load chapter content
    LaunchedEffect(currentChapterId) {
        Log.d("ChapterScreen", "Loading chapter: $currentChapterId")
        viewModel.loadChapterContent(currentChapterId)
        
        // Reset page index khi chapter thay đổi nếu không phải lần đầu load
        if (currentChapterId != chapterId) {
            initialPageIndex = 0
            
            // Reset scroll position
            when (readingMode) {
                ReadingMode.HORIZONTAL_SWIPE -> {
                    coroutineScope.launch {
                        pagerState.scrollToPage(0)
                    }
                }
                else -> {
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                }
            }
            viewModel.updateCurrentPage(1)
        }
    }
    // Show next chapter button when reaching near the end
    LaunchedEffect(listState, pagerState) {
        snapshotFlow {
            when (readingMode) {
                ReadingMode.HORIZONTAL_SWIPE -> pagerState.currentPage >= chapterImageUrls.lastIndex - 2
                else -> {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val lastVisibleItem = visibleItems.lastOrNull()
                    lastVisibleItem?.index ?: 0 >= chapterImageUrls.lastIndex - 2
                }
            }
        }.collect { isNearEnd ->
            viewModel.updateNextChapterButtonVisibility(isNearEnd)
        }
    }

    // Save reading progress
    LaunchedEffect(listState, pagerState) {
        snapshotFlow {
            when (readingMode) {
                ReadingMode.HORIZONTAL_SWIPE -> pagerState.currentPage
                else -> listState.firstVisibleItemIndex
            }
        }.collect { index ->
            if (index >= 0 && chapterImageUrls.isNotEmpty()) {
                viewModel.updateCurrentPage(index + 1)
                viewModel.saveReadingProgress(
                    mangaId = mangaId,
                    chapterId = currentChapterId,
                    language = language,
                    lastPageIndex = index + 1
                )
            }
        }
    }

    // Scroll to initial page
    LaunchedEffect(chapterImageUrls, readingMode) {
        if (chapterImageUrls.isNotEmpty() && pageIndex > 0) {
            when (readingMode) {
                ReadingMode.HORIZONTAL_SWIPE -> pagerState.scrollToPage(pageIndex)
                else -> listState.scrollToItem(pageIndex)
            }
            viewModel.updateCurrentPage(pageIndex + 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isZooming) viewModel.toggleControls()
                    },
                    onDoubleTap = { tapPosition ->
                        viewModel.hideControls()
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2.5f
                            val visiblePagePosition = pagePositions[when (readingMode) {
                                ReadingMode.HORIZONTAL_SWIPE -> pagerState.currentPage
                                else -> firstVisibleItemIndex
                            }] ?: Offset.Zero
                            val adjustedTapY = tapPosition.y + if (readingMode == ReadingMode.HORIZONTAL_SWIPE) 0f
                            else firstVisibleItemScrollOffset - visiblePagePosition.y
                            scale = targetScale
                            offset = Offset(
                                x = -(tapPosition.x - size.width / 2f) * (targetScale - 1),
                                y = -(adjustedTapY - size.height / 2f) * (targetScale - 1)
                            )
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    val prevScale = scale
                    val newScale = (scale * gestureZoom).coerceIn(minScale, maxScale)
                    scale = newScale

                    if (newScale > 1.05f) {
                        val zoomChange = newScale / prevScale
                        val adjustedCentroidY = centroid.y + if (readingMode == ReadingMode.HORIZONTAL_SWIPE) 0f
                        else firstVisibleItemScrollOffset
                        val newOffsetX = offset.x * zoomChange + pan.x + (1f - zoomChange) * (centroid.x - size.width / 2f)
                        val newOffsetY = offset.y * zoomChange + pan.y + (1f - zoomChange) * (adjustedCentroidY - size.height / 2f)

                        val maxPanX = size.width * (newScale - 1f)
                        val maxPanY = size.height * (newScale - 1f) * 2f

                        offset = Offset(
                            x = newOffsetX.coerceIn(-maxPanX, maxPanX),
                            y = newOffsetY.coerceIn(-maxPanY, maxPanY)
                        )
                    } else {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            when {
                chapterImageUrls.isEmpty() -> LoadingIndicator()
                else -> {
                    when (readingMode) {
                        ReadingMode.VERTICAL_SCROLL -> VerticalReader(
                            listState = listState,
                            chapterImageUrls = chapterImageUrls,
                            isZooming = isZooming,
                            pagePositions = pagePositions
                        )
                        ReadingMode.HORIZONTAL_SWIPE -> HorizontalReader(
                            pagerState = pagerState,
                            chapterImageUrls = chapterImageUrls,
                            isZooming = isZooming,
                            pagePositions = pagePositions
                        )
                        ReadingMode.CONTINUOUS_SCROLL -> ContinuousReader(
                            listState = listState,
                            chapterImageUrls = chapterImageUrls,
                            isZooming = isZooming,
                            pagePositions = pagePositions
                        )
                    }
                }
            }
        }

        // Top controls bar with animation
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = chapterTitle.ifEmpty { "Chapter" },
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    ReadingModeSelector(readingMode, onModeSelected = { mode ->
                        if (mode != readingMode) {
                            readingMode = mode
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                when (readingMode) {
                                    ReadingMode.HORIZONTAL_SWIPE -> pagerState.scrollToPage(firstVisibleItemIndex)
                                    else -> listState.scrollToItem(pagerState.currentPage)
                                }
                            }
                        }
                    })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }

        // Bottom controls container (contains page indicator and possibly chapter controls)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous page button (for horizontal mode)
                if (readingMode == ReadingMode.HORIZONTAL_SWIPE && pagerState.currentPage > 0) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Page"
                        )
                    }
                } else {
                    // Empty spacer for layout balance in other modes
                    Spacer(modifier = Modifier.width(56.dp))
                }

                // Page indicator - always visible
                PageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages
                )

                // Next page button (for horizontal mode)
                if (readingMode == ReadingMode.HORIZONTAL_SWIPE && pagerState.currentPage < chapterImageUrls.size - 1) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Page"
                        )
                    }
                } else {
                    // Empty spacer for layout balance in other modes
                    Spacer(modifier = Modifier.width(56.dp))
                }
            }
        }

        // Next chapter button with improved animation and visual indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showNextChapterButton && !isLoadingNextChapter,
                enter = fadeIn() + slideInVertically { it } + expandHorizontally(),
                exit = fadeOut() + slideOutVertically { it } + shrinkHorizontally()
            ) {
                NextChapterButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.loadNextChapter(currentChapterId, mangaId, language) { nextChapterId ->
                            if (nextChapterId != null && nextChapterId != currentChapterId) {
                                currentChapterId = nextChapterId
                                initialPageIndex = 0 // Reset the page index

                                // Reset scroll position
                                coroutineScope.launch {
                                    when (readingMode) {
                                        ReadingMode.HORIZONTAL_SWIPE -> pagerState.scrollToPage(0)
                                        else -> listState.scrollToItem(0)
                                    }
                                }

                                viewModel.updateCurrentPage(1) // Update the current page in ViewModel
                            } else {
                                Toast.makeText(
                                    context,
                                    "You've reached the latest chapter",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    isLoading = isLoadingNextChapter
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading chapter...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VerticalReader(
    listState: LazyListState,
    chapterImageUrls: List<String>,
    isZooming: Boolean,
    pagePositions: MutableMap<Int, Offset>
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !isZooming
    ) {
        itemsIndexed(chapterImageUrls) { index, imageUrl ->
            MangaImageItem(
                imageUrl = imageUrl,
                contentDescription = "Page ${index + 1}",
                onPositioned = { position, height ->
                    pagePositions[index] = position
                }
            )
        }
        // Add loading indicator at the end
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "End of Chapter",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalReader(
    pagerState: androidx.compose.foundation.pager.PagerState,
    chapterImageUrls: List<String>,
    isZooming: Boolean,
    pagePositions: MutableMap<Int, Offset>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZooming
        ) { page ->
            MangaImageItem(
                imageUrl = chapterImageUrls[page],
                contentDescription = "Page ${page + 1}",
                onPositioned = { position, height ->
                    pagePositions[page] = position
                }
            )
        }

        // Edge indicators
        AnimatedVisibility(
            visible = pagerState.currentPage > 0 && !isZooming,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .alpha(0.2f)
                    .background(Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = pagerState.currentPage < chapterImageUrls.size - 1 && !isZooming,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .alpha(0.2f)
                    .background(Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ContinuousReader(
    listState: LazyListState,
    chapterImageUrls: List<String>,
    isZooming: Boolean,
    pagePositions: MutableMap<Int, Offset>
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = !isZooming
    ) {
        itemsIndexed(chapterImageUrls) { index, imageUrl ->
            MangaImageItem(
                imageUrl = imageUrl,
                contentDescription = "Page ${index + 1}",
                onPositioned = { position, height ->
                    pagePositions[index] = position
                },
                continuous = true
            )
        }
        // Add loading indicator at the end
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "End of Chapter",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ReadingModeSelector(
    currentMode: ReadingMode,
    onModeSelected: (ReadingMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Reading mode",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            ReadingMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                val displayText = when (mode) {
                    ReadingMode.VERTICAL_SCROLL -> "Vertical Scroll"
                    ReadingMode.HORIZONTAL_SWIPE -> "Horizontal Paging"
                    ReadingMode.CONTINUOUS_SCROLL -> "Continuous Scroll"
                }

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayText,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(alpha = 0.7f),
        shadowElevation = 4.dp
    ) {
        Text(
            text = "$currentPage / $totalPages",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NextChapterButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(36.dp)
            .defaultMinSize(minWidth = 80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Next",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Next",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun MangaImageItem(
    imageUrl: String,
    contentDescription: String,
    onPositioned: (position: Offset, height: Int) -> Unit,
    continuous: Boolean = false
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (continuous) Modifier else Modifier.wrapContentHeight())
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInRoot(),
                    coordinates.size.height
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = if (continuous) ContentScale.Fit else ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            onSuccess = { isLoading = false },
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFF212121)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Loading image...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFF4A1111)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Failed to load image",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Failed to load image",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { /* Trigger reload */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        )
    }
}

