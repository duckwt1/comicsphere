package com.android.dacs3.presentations.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.android.dacs3.data.model.ChapterData
import com.android.dacs3.data.model.MangaDetailUiState
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.MangaViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun MangaDetailScreen(
    mangaId: String,
    navController: NavHostController,
    viewModel: MangaViewModel,
    onBackClick: () -> Unit
) {
    val mangaDetail by viewModel.mangaDetail.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val chapters by viewModel.chapters.collectAsState()

    val textColor = Color(0xFF000000)
    val systemUiController = rememberSystemUiController()

    var isDescriptionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mangaId, selectedLanguage) {
        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = true)
        viewModel.loadMangaDetails(mangaId)
        viewModel.loadChapters(mangaId,selectedLanguage)
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            MangaTopBar(onBackClick = onBackClick, textColor = textColor)
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            MangaHeader(
                mangaDetail = mangaDetail,
                selectedLanguage = selectedLanguage,
                availableLanguages = availableLanguages,
                onLanguageSelected = { viewModel.changeLanguage(it) },
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("chapter_screen/$mangaId/$selectedLanguage")
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Read Now")
            }

            Spacer(modifier = Modifier.height(16.dp))

            MangaDescription(
                description = mangaDetail.description,
                isExpanded = isDescriptionExpanded,
                onExpandToggle = { isDescriptionExpanded = !isDescriptionExpanded },
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            MangaTags(tags = mangaDetail.genres, textColor = textColor)

            Spacer(modifier = Modifier.height(16.dp))

            MangaChapters(chapters = chapters, mangaId, textColor = textColor,navController = navController, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaTopBar(onBackClick: () -> Unit, textColor: Color) {
    TopAppBar(
        title = { Text("", color = textColor) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F8F8))
    )
}

@Composable
fun MangaHeader(
    mangaDetail: MangaDetailUiState,
    selectedLanguage: String,
    availableLanguages: List<String>,
    onLanguageSelected: (String) -> Unit,
    textColor: Color
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
                textColor = textColor
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
    textColor: Color
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
                unfocusedBorderColor = textColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = textColor
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White) // Đặt màu nền sáng cho dropdown menu
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
    textColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
fun MangaTags(tags: List<String>, textColor: Color) {
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
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun MangaChapters(
    chapters: List<ChapterData>,
    mangaId: String,
    textColor: Color,
    navController: NavHostController,
    viewModel: MangaViewModel
) {
    val context = LocalContext.current

    Text(
        text = "Chapters:",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = textColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chapters
            .sortedByDescending { it.attributes.chapter?.toFloatOrNull() ?: 0f }
            .forEach { chapter ->
                TextButton(
                    onClick = {
                        val externalUrl = chapter.attributes.externalUrl
                        if (externalUrl != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl))
                            context.startActivity(intent)
                        } else {
                            navController.navigate(Screens.ChapterScreen.createRoute(mangaId ,chapter.id, chapter.attributes.translatedLanguage))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = textColor
                    )
                ) {
                    Text(
                        text = "Chapter ${chapter.attributes.chapter ?: "?"}: ${chapter.attributes.title.orEmpty()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
    }
}


