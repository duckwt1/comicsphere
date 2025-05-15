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

    // Custom light theme color scheme
    val customLightColorScheme = lightColorScheme(
        primary = Color(0xFF3B7FD9),           // Bright blue for primary actions
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE4F0FF),  // Light blue background
        secondary = Color(0xFF5E35B1),         // Purple for secondary elements
        secondaryContainer = Color(0xFFEDE5FF),
        tertiary = Color(0xFF388E3C),          // Green for success/price elements
        background = Color(0xFFF8F9FC),        // Very light blue-gray background
        surface = Color(0xFFFFFFFF),           // White surface
        surfaceVariant = Color(0xFFF3F6FF),    // Light blue-tinted variant
        error = Color(0xFFE53935)              // Red for errors
    )

    // Apply the custom theme
    MaterialTheme(colorScheme = customLightColorScheme) {
        LaunchedEffect(paymentState) {
            when (paymentState) {
                PaymentState.SUCCESS -> {
                    snackbarHostState.showSnackbar("Thanh toán thành công!")
                }
                PaymentState.ERROR -> {
                    snackbarHostState.showSnackbar("Thanh toán thất bại. Vui lòng thử lại.")
                }
                else -> {}
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Nâng cấp VIP",
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
                    text = if (isVip) "Gia hạn gói VIP" else "Chọn gói VIP phù hợp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // VIP Package Cards
                VipPackageCard(
                    title = "1 tháng",
                    price = "50,000đ",
                    isPopular = false,
                    onClick = {
                        viewModel.setSelectedMonths(1)
                        viewModel.setSelectedAmount(50000)
                        viewModel.setSelectedDescription("Nâng cấp VIP ComicSphere - 1 tháng")
                        viewModel.purchaseVip(context as Activity, 1, 50000)
                    },
                    isLoading = paymentState == PaymentState.LOADING && viewModel.selectedMonths.collectAsStateWithLifecycle().value == 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                VipPackageCard(
                    title = "3 tháng",
                    price = "135,000đ",
                    discount = "10%",
                    isPopular = true,
                    onClick = {
                        viewModel.setSelectedMonths(3)
                        viewModel.setSelectedAmount(135000)
                        viewModel.setSelectedDescription("Nâng cấp VIP ComicSphere - 3 tháng")
                        viewModel.purchaseVip(context as Activity, 3, 135000)
                    },
                    isLoading = paymentState == PaymentState.LOADING && viewModel.selectedMonths.collectAsStateWithLifecycle().value == 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                VipPackageCard(
                    title = "12 tháng",
                    price = "480,000đ",
                    discount = "20%",
                    isPopular = false,
                    onClick = {
                        viewModel.setSelectedMonths(12)
                        viewModel.setSelectedAmount(480000)
                        viewModel.setSelectedDescription("Nâng cấp VIP ComicSphere - 12 tháng")
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
            colors = listOf(Color(0xFF5E35B1), Color(0xFF3B7FD9))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF3B7FD9), Color(0xFF3F51B5))
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
                    text = "Bạn đang là thành viên VIP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val expireDateStr = dateFormat.format(Date(vipExpireDate))

                Text(
                    text = "Hết hạn: $expireDateStr",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "Trải nghiệm đặc quyền VIP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Nâng cao trải nghiệm đọc truyện của bạn",
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Đặc quyền thành viên VIP:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Liệt kê các đặc quyền VIP với icon
            VipBenefitItem("Không hiển thị quảng cáo")
            VipBenefitItem("Truy cập tất cả truyện độc quyền")
            VipBenefitItem("Tải truyện để đọc offline")
            VipBenefitItem("Hỗ trợ ưu tiên 24/7")
            VipBenefitItem("Cập nhật truyện mới sớm nhất")
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
            style = MaterialTheme.typography.bodyLarge
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
                MaterialTheme.colorScheme.primaryContainer
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
                        "Phổ biến nhất",
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
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Tiết kiệm $discount",
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
                        MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Chọn gói này",
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
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Đang xử lý thanh toán...",
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
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "Thanh toán thất bại. Vui lòng thử lại.",
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
                color = Color(0xFFDCEDC8) // Light green for success
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thanh toán thành công!",
                        color = Color(0xFF388E3C),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> { /* Không hiển thị gì */ }
    }
}

