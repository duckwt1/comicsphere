package com.android.dacs3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.android.dacs3.data.repository.ZaloPayRepository
import com.android.dacs3.presentations.components.NetworkAwareContent
import com.android.dacs3.presentations.navigation.AppNavGraph
import com.android.dacs3.ui.theme.DACS3Theme
import com.android.dacs3.viewmodel.VipViewModel
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val vipViewModel: VipViewModel by viewModels()
    
    @Inject
    lateinit var zaloPayRepository: ZaloPayRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo ZaloPay SDK
        zaloPayRepository.initSdk(this)
        
        // Xử lý intent khi ứng dụng được mở từ deep link
        handleIntent(intent)
        
        // Window mode, hide system bars, navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            DACS3Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NetworkAwareContent {
                        AppNavGraph(navController)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Chuyển kết quả cho ZaloPay xử lý
        zaloPayRepository.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with intent: $intent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        try {
            val uri = intent.data
            Log.d("MainActivity", "Handling intent with URI: $uri")
            
            if (uri != null && uri.scheme == "comicsphere" && uri.host == "zalopay.callback") {
                // Xử lý callback từ ZaloPay
                val code = uri.getQueryParameter("code")
                val isSuccess = code == "1" // 1 = success
                Log.d("MainActivity", "Received ZaloPay callback, code: $code, isSuccess: $isSuccess")

                // Sử dụng ZaloPayRepository để xử lý callback
                val processed = zaloPayRepository.processZaloPayCallback(uri)

                Log.d("MainActivity", "ZaloPay callback processed: $processed")
                
                // Thông báo cho VipViewModel
                if (isSuccess) {
                    vipViewModel.handleZaloPayResult(true)
                    Log.d("MainActivity", "Notified VipViewModel of successful payment")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling intent", e)
        }
    }
}




