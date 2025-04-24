package com.android.dacs3.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.financial_app.presentation.navigation.BottomNavigationBar

@Composable
fun FavouriteScreen(navController: NavController){
    Scaffold(
        topBar = { SearchBar()},
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {


        }
    }
}