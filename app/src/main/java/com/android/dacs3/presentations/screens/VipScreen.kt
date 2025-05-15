package com.android.dacs3.presentations.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.dacs3.presentations.components.PaymentButton
import com.android.dacs3.viewmodel.PaymentState
import com.android.dacs3.viewmodel.VipViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipScreen(
    viewModel: VipViewModel = hiltViewModel(),
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val paymentState by viewModel.paymentState.collectAsState()
    val isVip by viewModel.isVip.observeAsState(initial = false)
    val vipExpireDate by viewModel.vipExpireDate.observeAsState(initial = 0L)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Light theme color scheme
    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF1976D2),           // Blue for primary actions
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3F2FD),  // Light blue background
        secondary = Color(0xFF673AB7),         // Purple for secondary elements
        secondaryContainer = Color(0xFFEDE7F6), // Light purple background
        tertiary = Color(0xFF2E7D32),          // Green for success/price elements
        background = Color.White,              // White background
        surface = Color(0xFFF5F5F5),           // Light gray surface
        surfaceVariant = Color(0xFFEEEEEE),    // Slightly darker variant
        error = Color(0xFFD32F2F)              // Red for errors
    )

    // Apply the light theme
    MaterialTheme(colorScheme = lightColorScheme) {
        LaunchedEffect(paymentState) {
            when (paymentState) {
                PaymentState.SUCCESS -> {
                    snackbarHostState.showSnackbar("Payment successful!")
                }
                PaymentState.ERROR -> {
                    snackbarHostState.showSnackbar("Payment failed. Please try again.")
                }
                else -> {}
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "VIP Upgrade",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // VIP Header Banner
                VipHeaderBanner(isVip, vipExpireDate)

                Spacer(modifier = Modifier.height(24.dp))

                // VIP Benefits Section
                if (!isVip) {
                    VipBenefitsSection()
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // VIP Package Options
                Text(
                    text = if (isVip) "Renew VIP Subscription" else "Choose Your VIP Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                // VIP Package Cards
                VipPackageCard(
                    title = "1 Month",
                    price = "50,000đ",
                    isPopular = false,
                    onClick = {
                        viewModel.setSelectedMonths(1)
                        viewModel.setSelectedAmount(50000)
                        viewModel.setSelectedDescription("ComicSphere VIP Upgrade - 1 Month")
                        viewModel.purchaseVip(context as Activity, 1, 50000)
                    },
                    isLoading = paymentState == PaymentState.LOADING && viewModel.selectedMonths.collectAsStateWithLifecycle().value == 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                VipPackageCard(
                    title = "3 Months",
                    price = "135,000đ",
                    discount = "10%",
                    isPopular = true,
                    onClick = {
                        viewModel.setSelectedMonths(3)
                        viewModel.setSelectedAmount(135000)
                        viewModel.setSelectedDescription("ComicSphere VIP Upgrade - 3 Months")
                        viewModel.purchaseVip(context as Activity, 3, 135000)
                    },
                    isLoading = paymentState == PaymentState.LOADING && viewModel.selectedMonths.collectAsStateWithLifecycle().value == 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                VipPackageCard(
                    title = "12 Months",
                    price = "480,000đ",
                    discount = "20%",
                    isPopular = false,
                    onClick = {
                        viewModel.setSelectedMonths(12)
                        viewModel.setSelectedAmount(480000)
                        viewModel.setSelectedDescription("ComicSphere VIP Upgrade - 12 Months")
                        viewModel.purchaseVip(context as Activity, 12, 480000)
                    },
                    isLoading = paymentState == PaymentState.LOADING && viewModel.selectedMonths.collectAsStateWithLifecycle().value == 12
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Payment Status Indicator
                PaymentStatusIndicator(paymentState)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun VipHeaderBanner(isVip: Boolean, vipExpireDate: Long) {
    val gradientBrush = if (isVip) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF9575CD), Color(0xFF7986CB))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF7986CB), Color(0xFF5C6BC0))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "VIP Icon",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isVip) {
                Text(
                    text = "You are a VIP Member",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                val expireDateStr = dateFormat.format(Date(vipExpireDate))

                Text(
                    text = "Expires: $expireDateStr",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "Experience VIP Privileges",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Enhance your reading experience",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun VipBenefitsSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "VIP Member Benefits:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List VIP benefits with icons
            VipBenefitItem("Ad-free experience")
            VipBenefitItem("Access to all exclusive comics")
            VipBenefitItem("Download comics for offline reading")
            VipBenefitItem("Priority 24/7 support")
            VipBenefitItem("Early access to new releases")
        }
    }
}

@Composable
fun VipBenefitItem(benefit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = benefit,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VipPackageCard(
    title: String,
    price: String,
    discount: String? = null,
    isPopular: Boolean = false,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isPopular) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPopular) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        "Most Popular",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isPopular) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = price,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.tertiary
            )

            if (discount != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Text(
                        text = "Save $discount",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Select Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentStatusIndicator(paymentState: PaymentState) {
    when (paymentState) {
        PaymentState.LOADING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Processing payment...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PaymentState.ERROR -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFEBEE)
            ) {
                Text(
                    text = "Payment failed. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        PaymentState.SUCCESS -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9) // Light green for success
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment successful!",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> { /* Don't display anything */ }
    }
}