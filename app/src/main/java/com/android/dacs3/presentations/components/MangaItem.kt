package com.android.dacs3.presentations.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.dacs3.data.model.MangaData
import com.android.dacs3.data.model.displayCoverUrl
import com.android.dacs3.utliz.Screens
import android.util.Log

@Composable
fun MangaItem(
    manga: MangaData,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val title = manga.attributes.title["en"] ?: "Unknown Title"
    val imageUrl = manga.displayCoverUrl
    
    // Log URL ảnh bìa
    LaunchedEffect(manga.id) {
        Log.d("MangaItem", "Manga ${manga.id} - Title: $title - Cover URL: $imageUrl")
    }
    
    Column(
        modifier = modifier
            .width(120.dp)
            .padding(8.dp)
            .clickable {
                navController.navigate(Screens.DetailsScreen.createRoute(manga.id))
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
