package com.android.dacs3.presentations.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.dacs3.R
import com.android.dacs3.data.model.Comment
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentSection(
    comments: List<Comment>,
    currentUserId: String?,
    isLoading: Boolean,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Header
        Text(
            text = "Comments (${comments.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Comments list
        if (comments.isEmpty() && !isLoading) {
            EmptyCommentsPlaceholder()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isOwnComment = comment.userId == currentUserId,
                            onDeleteComment = { onDeleteComment(comment.id) }
                        )

                        if (comments.indexOf(comment) < comments.lastIndex) {
                            Divider(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Removed global loading indicator since we now show it on the button

        Spacer(modifier = Modifier.height(16.dp))

        // Comment input field - only shown if user is logged in
        if (currentUserId != null) {
            CommentInput(
                onSendComment = onAddComment,
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun EmptyCommentsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No comments yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF000000)
                )

                Text(
                    text = "Be the first to share your thoughts!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF000000).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInput(
    onSendComment: (String) -> Unit,
    isLoading: Boolean
) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        "Add a comment...",
                        color = Color(0xFF000000).copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                maxLines = 1,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF000000),
                    unfocusedBorderColor = Color(0xFF000000).copy(alpha = 0.5f),
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000),
                    cursorColor = Color(0xFF000000)
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            Button(
                onClick = {
                    if (commentText.isNotBlank() && !isLoading) {
                        onSendComment(commentText)
                        commentText = ""
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = commentText.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwnComment: Boolean,
    onDeleteComment: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Avatar
        AsyncImage(
            model = comment.avatar,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.default_avatar),
            fallback = painterResource(id = R.drawable.default_avatar),
            placeholder = painterResource(id = R.drawable.default_avatar)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Comment content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Username, timestamp and delete option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.nickname,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // Delete icon only for user's own comments
                    if (isOwnComment) {
                        IconButton(
                            onClick = onDeleteComment,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete comment",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Comment text
            Text(
                text = comment.comment,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFF000000)
            )
        }
    }
}

// Time formatter
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}