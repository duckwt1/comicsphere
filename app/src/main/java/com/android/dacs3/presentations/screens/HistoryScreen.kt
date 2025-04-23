package com.android.dacs3.presentations.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Label
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun historyScreen(navController: NavController) {
    Text(text = "History Screen")
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun HistoryScreenPreview() {
    historyScreen(navController = rememberNavController())
}