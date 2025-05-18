package com.android.dacs3.presentations.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.dacs3.data.model.User
import com.android.dacs3.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

// Define black and white theme colors
private val BlackWhiteTheme = object {
    val primary = Color.Black
    val onPrimary = Color.White
    val background = Color.White
    val surface = Color.White
    val onSurface = Color.Black
    val border = Color.Black
    val divider = Color.LightGray
    val iconTint = Color.Black
    val vipIndicator = Color.Black
    val adminIndicator = Color.Black
    val error = Color.Black
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlackWhiteTheme.primary,
                    titleContentColor = BlackWhiteTheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BlackWhiteTheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BlackWhiteTheme.background)
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, BlackWhiteTheme.border, RoundedCornerShape(8.dp)),
                placeholder = { Text("Search users...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BlackWhiteTheme.iconTint
                    )
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = BlackWhiteTheme.iconTint
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    cursorColor = BlackWhiteTheme.primary,
                    focusedBorderColor = BlackWhiteTheme.primary,
                    unfocusedBorderColor = BlackWhiteTheme.border
                )
            )

            // User list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlackWhiteTheme.primary)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "An error occurred",
                        color = BlackWhiteTheme.error
                    )
                }
            } else {
                val filteredUsers = viewModel.getFilteredUsers()

                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.searchQuery.isEmpty())
                                "No users found"
                            else
                                "No users matching '${viewModel.searchQuery}'",
                            color = BlackWhiteTheme.divider
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserListItem(
                                user = user,
                                onClick = {
                                    selectedUser = user
                                    showUserDetailsDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // User details dialog
    if (showUserDetailsDialog && selectedUser != null) {
        UserDetailsDialog(
            user = selectedUser!!,
            onDismiss = { showUserDetailsDialog = false },
            onToggleVip = { user, months ->
                viewModel.toggleVipStatus(user, months)
            },
            onToggleAdmin = { user ->
                viewModel.toggleAdminStatus(user)
            },
            onDeleteUser = {
                showUserDetailsDialog = false
                showDeleteConfirmDialog = true
            },
            onEditUser = { user ->
                showEditUserDialog = true
            }
        )
    }

    // Edit user dialog
    if (showEditUserDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = { showEditUserDialog = false },
            onSave = { fullname, nickname ->
                viewModel.updateUserInfo(selectedUser!!, fullname, nickname)
                showEditUserDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = BlackWhiteTheme.surface,
            titleContentColor = BlackWhiteTheme.onSurface,
            textContentColor = BlackWhiteTheme.onSurface,
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete ${selectedUser!!.fullname}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(selectedUser!!)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.error,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BlackWhiteTheme.primary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BlackWhiteTheme.border)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlackWhiteTheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BlackWhiteTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.avatar.ifEmpty { "https://ui-avatars.com/api/?name=${user.fullname}&background=random" })
                    .crossfade(true)
                    .build(),
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(1.dp, BlackWhiteTheme.border, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.fullname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = BlackWhiteTheme.onSurface
                )

                Text(
                    text = user.email,
                    fontSize = 14.sp,
                    color = BlackWhiteTheme.divider,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status indicators
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.isVip) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "VIP User",
                        tint = BlackWhiteTheme.vipIndicator,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (user.isAdmin) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Admin User",
                        tint = BlackWhiteTheme.adminIndicator,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserDetailsDialog(
    user: User,
    onDismiss: () -> Unit,
    onToggleVip: (User, Int) -> Unit,
    onToggleAdmin: (User) -> Unit,
    onDeleteUser: () -> Unit,
    onEditUser: (User) -> Unit
) {
    var showVipOptions by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BlackWhiteTheme.surface
            ),
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatar.ifEmpty { "https://ui-avatars.com/api/?name=${user.fullname}&background=random" })
                        .crossfade(true)
                        .build(),
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, BlackWhiteTheme.border, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User info
                Text(
                    text = user.fullname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = BlackWhiteTheme.onSurface
                )

                Text(
                    text = user.email,
                    fontSize = 16.sp,
                    color = BlackWhiteTheme.divider
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.isVip) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "VIP User",
                                tint = BlackWhiteTheme.vipIndicator
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VIP", color = BlackWhiteTheme.vipIndicator)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    if (user.isAdmin) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Admin User",
                                tint = BlackWhiteTheme.adminIndicator
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin", color = BlackWhiteTheme.adminIndicator)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Button(
                    onClick = { showVipOptions = !showVipOptions },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.primary,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (user.isVip) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = "Toggle VIP"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (user.isVip) "Remove VIP Status" else "Grant VIP Status")
                }

                // VIP options
                if (showVipOptions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Select VIP Duration:",
                            fontWeight = FontWeight.Bold,
                            color = BlackWhiteTheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            VipDurationButton(1, "1 Month") {
                                onToggleVip(user, 1)
                                showVipOptions = false
                            }

                            VipDurationButton(3, "3 Months") {
                                onToggleVip(user, 3)
                                showVipOptions = false
                            }

                            VipDurationButton(6, "6 Months") {
                                onToggleVip(user, 6)
                                showVipOptions = false
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onToggleAdmin(user) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.primary,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (user.isAdmin) Icons.Default.Person else Icons.Default.Person,
                        contentDescription = "Toggle Admin"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (user.isAdmin) "Remove Admin Status" else "Grant Admin Status")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add Edit button
                Button(
                    onClick = {
                        onDismiss()
                        onEditUser(user)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.primary,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit User"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit User")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDeleteUser,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlackWhiteTheme.error,
                        contentColor = BlackWhiteTheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete User"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete User")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BlackWhiteTheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun VipDurationButton(months: Int, text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BlackWhiteTheme.primary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(BlackWhiteTheme.border)
        )
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var fullname by remember { mutableStateOf(user.fullname) }
    var nickname by remember { mutableStateOf(user.nickname) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = BlackWhiteTheme.surface
            ),
            border = BorderStroke(1.dp, BlackWhiteTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Edit User",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BlackWhiteTheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullname,
                    onValueChange = { fullname = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = BlackWhiteTheme.primary,
                        focusedBorderColor = BlackWhiteTheme.primary,
                        unfocusedBorderColor = BlackWhiteTheme.border,
                        focusedLabelColor = BlackWhiteTheme.primary,
                        unfocusedLabelColor = BlackWhiteTheme.border
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        cursorColor = BlackWhiteTheme.primary,
                        focusedBorderColor = BlackWhiteTheme.primary,
                        unfocusedBorderColor = BlackWhiteTheme.border,
                        focusedLabelColor = BlackWhiteTheme.primary,
                        unfocusedLabelColor = BlackWhiteTheme.border
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = BlackWhiteTheme.primary
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSave(fullname, nickname) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlackWhiteTheme.primary,
                            contentColor = BlackWhiteTheme.onPrimary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}