package com.android.dacs3.presentations.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.android.dacs3.viewmodel.MangaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChapterScreen(
    mangaId: String,
    chapterId: String,
    language: String,
    navController: NavHostController,
    viewModel: MangaViewModel
) {
    val context = LocalContext.current
    val chapterImageUrls by viewModel.chapterImageUrls.collectAsState()
    val currentPage by viewModel.currentPageReading.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var currentChapterId by remember { mutableStateOf(chapterId) }

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
    val pageHeights = remember { mutableStateMapOf<Int, Int>() }
    val isZooming by remember { derivedStateOf { scale > 1.05f } }
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    var showControls by remember { mutableStateOf(false) }
    var showNextChapterButton by remember { mutableStateOf(false) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(currentChapterId) {
        viewModel.loadChapterContent(currentChapterId)
    }

    LaunchedEffect(firstVisibleItemIndex) {
        if (chapterImageUrls.isNotEmpty()) {
            val visiblePage = firstVisibleItemIndex + 1
            viewModel.updateCurrentPage(visiblePage)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val lastVisibleItem = visibleItems.lastOrNull()
            lastVisibleItem?.index == chapterImageUrls.lastIndex
        }.collect { isAtEnd ->
            showNextChapterButton = isAtEnd
        }
    }

    LaunchedEffect(animatedScale, offset) {
        if (animatedScale <= 1.05f && firstVisibleItemIndex >= 0 && pagePositions.isNotEmpty()) {
            offset = Offset.Zero
        }
    }

    var lastVisibleIndex by remember { mutableStateOf(0) }
    LaunchedEffect(firstVisibleItemIndex) {
        if (lastVisibleIndex != firstVisibleItemIndex && isZooming) {
            scale = 1f
            offset = Offset.Zero
        }
        lastVisibleIndex = firstVisibleItemIndex
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isZooming) {
                            showControls = !showControls
                        }
                    },
                    onDoubleTap = { tapPosition ->
                        showControls = false
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2.5f
                            val visiblePagePosition = pagePositions[firstVisibleItemIndex] ?: Offset.Zero
                            val adjustedTapY = tapPosition.y + firstVisibleItemScrollOffset - visiblePagePosition.y
                            scale = targetScale
                            offset = Offset(
                                x = -(tapPosition.x - size.width / 2f) * (targetScale - 1),
                                y = -(adjustedTapY - size.height / 2f) * (targetScale - 1)
                            )
                        }
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
                        val adjustedCentroidY = centroid.y + firstVisibleItemScrollOffset
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
            if (chapterImageUrls.isEmpty()) {
                Text(
                    text = "Loading...",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            } else {
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
                                pageHeights[index] = height
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .background(Color(0x88000000))
                .padding(vertical = 4.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Page $currentPage / $totalPages",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        if (showNextChapterButton) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        viewModel.loadNextChapter(currentChapterId, mangaId, language) { nextChapterId ->
                            if (nextChapterId != null && nextChapterId != currentChapterId) {
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                    delay(100)
                                    currentChapterId = nextChapterId
                                }
                                showNextChapterButton = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "You have reached the last chapter.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Next Chapter")
                }
            }
        }
    }
}

@Composable
fun MangaImageItem(
    imageUrl: String,
    contentDescription: String,
    onPositioned: (position: Offset, height: Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFF303030))
                ) {
                    Text(
                        text = "Loading...",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
            }
        )
    }
}
