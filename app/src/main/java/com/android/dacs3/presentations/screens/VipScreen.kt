package com.android.dacs3.presentations.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(paymentState) {
        if (paymentState == PaymentState.SUCCESS) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Thanh toán thành công!")
            }
        } else if (paymentState == PaymentState.ERROR) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Thanh toán thất bại. Vui lòng thử lại.")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nâng cấp VIP") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hiển thị trạng thái VIP hiện tại
            if (isVip) {
                Text(
                    text = "Bạn đang là thành viên VIP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val expireDateStr = dateFormat.format(Date(vipExpireDate))

                Text(
                    text = "Hết hạn: $expireDateStr",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Gia hạn gói VIP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Nâng cấp tài khoản lên VIP",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Đặc quyền thành viên VIP:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Liệt kê các đặc quyền VIP
                listOf(
                    "Không hiển thị quảng cáo",
                    "Truy cập tất cả truyện độc quyền",
                    "Tải truyện để đọc offline",
                    "Hỗ trợ ưu tiên"
                ).forEach { benefit ->
                    Text(
                        text = "• $benefit",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Các gói VIP
            VipPackage(
                title = "1 tháng",
                price = "50,000đ",
                onClick = {
                    viewModel.purchaseVip(context as Activity, 1, 50000)
                },
                isLoading = paymentState == PaymentState.LOADING
            )

            Spacer(modifier = Modifier.height(16.dp))

            VipPackage(
                title = "3 tháng",
                price = "135,000đ",
                discount = "10%",
                onClick = {
                    viewModel.purchaseVip(context as Activity, 3, 135000)
                },
                isLoading = paymentState == PaymentState.LOADING
            )

            Spacer(modifier = Modifier.height(16.dp))

            VipPackage(
                title = "12 tháng",
                price = "480,000đ",
                discount = "20%",
                onClick = {
                    viewModel.purchaseVip(context as Activity, 12, 480000)
                },
                isLoading = paymentState == PaymentState.LOADING
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Thêm PaymentButton như trong ví dụ của bạn
            PaymentButton(
                amount = "50000",
                description = "Nâng cấp VIP ComicSphere - 1 tháng",
                onPaymentInitiated = {
                    viewModel.setPaymentState(PaymentState.LOADING)
                },
                onPaymentSuccess = { transactionId ->
                    viewModel.handleZaloPayResult(true)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Thanh toán thành công! Mã giao dịch: $transactionId")
                    }
                },
                onPaymentError = { error ->
                    viewModel.handleZaloPayResult(false)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Lỗi thanh toán: $error")
                    }
                },
                onPaymentCanceled = {
                    viewModel.setPaymentState(PaymentState.CANCELED)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Thanh toán đã bị hủy")
                    }
                }
            )

            // Hiển thị trạng thái thanh toán
            when (paymentState) {
                PaymentState.LOADING -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Đang xử lý thanh toán...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                PaymentState.ERROR -> {
                    Text(
                        text = "Thanh toán thất bại. Vui lòng thử lại.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                PaymentState.SUCCESS -> {
                    Text(
                        text = "Thanh toán thành công!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> { /* Không hiển thị gì */ }
            }
        }
    }
}

@Composable
fun VipPackage(
    title: String,
    price: String,
    discount: String? = null,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(price, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF388E3C))
            if (discount != null) {
                Text(
                    text = "Giảm $discount",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Mua", fontWeight = FontWeight.Bold)
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
