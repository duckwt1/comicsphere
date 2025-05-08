package com.android.dacs3.presentations.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.dacs3.utliz.connectivityState
import kotlinx.coroutines.delay

@Composable
fun NetworkAwareContent(
    content: @Composable () -> Unit
) {
    val isConnected by connectivityState()
    var showNetworkMessage by remember { mutableStateOf(false) }
    
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            showNetworkMessage = true
        } else if (showNetworkMessage) {
            // Delay before hiding the message when connection is restored
            delay(3000)
            showNetworkMessage = false
        }
    }
    
    Column {
        AnimatedVisibility(
            visible = !isConnected || showNetworkMessage,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (!isConnected) Color.Red else Color.Green)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!isConnected) 
                        "No internet connection" 
                    else 
                        "Connection restored",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        content()
    }
}