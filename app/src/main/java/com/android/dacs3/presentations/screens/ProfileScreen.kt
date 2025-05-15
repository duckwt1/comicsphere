package com.android.dacs3.presentations.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.android.dacs3.presentations.components.StyledTextField
import com.android.dacs3.presentations.navigation.BottomNavigationBar
import com.android.dacs3.utliz.Screens
import com.android.dacs3.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF8F8F8)
                )
                .padding(innerPadding)
        ) {
            when {
                viewModel.isLoading -> {
                    LoadingView()
                }
                viewModel.loginState.isNotEmpty() -> {
                    ErrorView(errorMessage = viewModel.loginState)
                }
                else -> {
                    ProfileContent(
                        viewModel = viewModel,
                        imagePicker = imagePicker,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
private fun ErrorView(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ProfileContent(
    viewModel: AuthViewModel,
    imagePicker: androidx.activity.result.ActivityResultLauncher<String>,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // User avatar section
        ProfileAvatar(
            avatarUrl = viewModel.currentUser?.avatar,
            isUpdating = viewModel.isUpdatingAvatar,
            onEditClick = { imagePicker.launch("image/*") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // User name with welcome text
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Text(
            text = viewModel.currentUser?.fullname ?: "User",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile information card
        ProfileInfoCard(viewModel = viewModel, navController = navController)

        Spacer(modifier = Modifier.height(24.dp))

        // VIP Button
        VipButton(
            isVip = viewModel.currentUser?.isVip == true,
            navController = navController
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Logout button
        LogoutButton(
            viewModel = viewModel,
            navController = navController
        )
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    isUpdating: Boolean,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .padding(4.dp)
    ) {
        // Avatar container with border
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(4.dp, CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Avatar",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Black
                )
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Loading overlay
            AnimatedVisibility(
                visible = isUpdating,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Edit button
        if (!isUpdating) {
            FloatingActionButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp),
                containerColor = Color.LightGray,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Avatar",
                    modifier = Modifier.size(15.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(viewModel: AuthViewModel, navController: NavController) {
    var isEditing by remember { mutableStateOf(false) }
    var fullname by remember { mutableStateOf(viewModel.currentUser?.fullname ?: "") }
    var nickname by remember { mutableStateOf(viewModel.currentUser?.nickname ?: "") }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Update local state when user data changes
    LaunchedEffect(viewModel.currentUser) {
        viewModel.currentUser?.let {
            fullname = it.fullname
            nickname = it.nickname
        }
    }
    
    // Handle profile update success
    LaunchedEffect(viewModel.updateProfileState) {
        if (viewModel.isProfileUpdateSuccessful) {
            isEditing = false
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x40000000)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                
                if (isEditing) {
                    Row {
                        IconButton(
                            onClick = {
                                isEditing = false
                                fullname = viewModel.currentUser?.fullname ?: ""
                                nickname = viewModel.currentUser?.nickname ?: ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.Gray
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                showError = fullname.isBlank() || nickname.isBlank()
                                if (!showError) {
                                    viewModel.updateUserProfile(fullname, nickname)
                                    isLoading = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Save",
                                tint = Color.Black
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { isEditing = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                // Editable fields
                Column {
                    StyledTextField(
                        value = fullname,
                        onValueChange = { 
                            fullname = it
                            showError = false
                        },
                        label = "Full Name",
                        hasError = showError && fullname.isBlank()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StyledTextField(
                        value = nickname,
                        onValueChange = { 
                            nickname = it
                            showError = false
                        },
                        label = "Nickname",
                        hasError = showError && nickname.isBlank()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StyledTextField(
                        value = viewModel.currentUser?.email ?: "",
                        onValueChange = { },
                        label = "Email",
                        isPassword = false,
                        hasError = false
                    )
                    
                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please fill in all required fields",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                    
                    if (viewModel.updateProfileState.isNotEmpty() && !viewModel.isProfileUpdateSuccessful) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.updateProfileState,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Read-only display
                ProfileInfoItem(
                    icon = Icons.Rounded.Person,
                    label = "Full Name",
                    value = viewModel.currentUser?.fullname ?: "N/A"
                )

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                ProfileInfoItem(
                    icon = Icons.Rounded.Face,
                    label = "Nickname",
                    value = viewModel.currentUser?.nickname ?: "N/A"
                )

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                ProfileInfoItem(
                    icon = Icons.Rounded.Email,
                    label = "Email",
                    value = viewModel.currentUser?.email ?: "N/A"
                )
            }
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = value.ifBlank { "N/A" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun LogoutButton(viewModel: AuthViewModel, navController: NavController) {
    val context = LocalContext.current
    
    Button(
        onClick = {
            viewModel.logout(context)
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(Screens.SplashScreen.route) { inclusive = true }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Red,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout Icon",
                tint = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Logout",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun VipButton(isVip: Boolean, navController: NavController) {
    Button(
        onClick = { navController.navigate(Screens.VipScreen.route) },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isVip) Color(0xFFFFD700) else Color(0xFF6200EE),
            contentColor = if (isVip) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "VIP Icon",
                tint = if (isVip) Color.Black else Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isVip) "Manage VIP Membership" else "Become VIP Member",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
