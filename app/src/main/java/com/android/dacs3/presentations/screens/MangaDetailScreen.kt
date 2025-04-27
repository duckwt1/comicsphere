package com.android.dacs3.presentations.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import com.android.dacs3.viewmodel.MangaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    navController: NavHostController,
    viewModel: MangaViewModel,
    onBackClick: () -> Unit
) {
    val mangaDetail by viewModel.mangaDetail.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState() // Lấy danh sách ngôn ngữ từ mangaDetail

    var expanded by remember { mutableStateOf(false) } // Quản lý trạng thái mở/đóng dropdown

    // Reload manga details when mangaId or selectedLanguage changes
    LaunchedEffect(mangaId, selectedLanguage) {
        Log.d("MangaDetailScreen", "Loading manga details for ID: $mangaId, Language: $selectedLanguage")
        viewModel.loadMangaDetails(mangaId) // Reload details with the selected language
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Image + Info Row
            Row(
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = mangaDetail.coverImageUrl,
                    contentDescription = mangaDetail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mangaDetail.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 2
                    )

                    Text(
                        text = "Author: ${mangaDetail.author}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Status: ${mangaDetail.status}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Language selection using ExposedDropdownMenu
                    // Language selection using ExposedDropdownMenu
                    Text("Select Language:", fontWeight = FontWeight.Bold)

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = if (availableLanguages.isNotEmpty()) selectedLanguage else "No language available",
                            onValueChange = {},
                            label = { Text("Language") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (availableLanguages.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No languages available") },
                                    onClick = { expanded = false }
                                )
                            } else {
                                availableLanguages.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language) },
                                        onClick = {
                                            viewModel.changeLanguage(language)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue Button
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Tiếp tục Chương 1")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Genres
            Text(
                text = "Genres:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                mangaDetail.genres.forEach { genre ->
                    AssistChip(
                        onClick = { },
                        label = { Text(text = genre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Description:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = mangaDetail.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
