package com.android.dacs3.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.dacs3.data.repository.AuthRepository
import com.android.dacs3.data.repository.ZaloPayRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class VipViewModel @Inject constructor(
    private val zaloPayRepository: ZaloPayRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _isVip = MutableLiveData<Boolean>()
    val isVip: LiveData<Boolean> = _isVip

    private val _vipExpireDate = MutableLiveData<Long>()
    val vipExpireDate: LiveData<Long> = _vipExpireDate

    // Lưu thông tin số tháng VIP cho mỗi giao dịch
    private val _selectedMonths = MutableStateFlow(1)
    val selectedMonths: StateFlow<Int> = _selectedMonths.asStateFlow()
    
    // Thêm state cho số tiền và mô tả
    private val _selectedAmount = MutableStateFlow(50000L)
    val selectedAmount: StateFlow<Long> = _selectedAmount.asStateFlow()
    
    private val _selectedDescription = MutableStateFlow("Nâng cấp VIP ComicSphere - 1 tháng")
    val selectedDescription: StateFlow<String> = _selectedDescription.asStateFlow()

    init {
        checkVipStatus()
    }

    private fun checkVipStatus() {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                
                if (userId == null) {
                    Log.e("VipViewModel", "User ID is null, cannot check VIP status")
                    return@launch
                }
                
                Log.d("VipViewModel", "Checking VIP status for user $userId")
                
                val result = zaloPayRepository.getVipStatus(userId)
                result.onSuccess { (isVip, expireDate) ->
                    Log.d("VipViewModel", "VIP status: isVip=$isVip, expireDate=$expireDate (${java.util.Date(expireDate)})")
                    _isVip.value = isVip
                    _vipExpireDate.value = expireDate
                }.onFailure { error ->
                    Log.e("VipViewModel", "Error getting VIP status", error)
                }
            } catch (e: Exception) {
                Log.e("VipViewModel", "Error in checkVipStatus", e)
            }
        }
    }

    fun purchaseVip(activity: Activity, months: Int, amount: Long) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.LOADING

            try {
                val description = _selectedDescription.value
                Log.d("VipViewModel", "Creating order for $months months, amount: $amount, description: $description")
                val orderResult = zaloPayRepository.createOrder(amount, description)

                if (orderResult.isSuccess) {
                    val token = orderResult.getOrThrow()
                    Log.d("VipViewModel", "Order created successfully with token: $token, initiating payment for $months months")
                    
                    // Lưu số tháng cho token này
                    zaloPayRepository.saveMonthsForToken(token, months)
                    
                    val payResult = zaloPayRepository.payOrder(activity, token, months)

                    if (payResult.isSuccess) {
                        // Thanh toán đã được khởi tạo thành công
                        // Kết quả thực tế sẽ được xử lý trong callback
                        Log.d("VipViewModel", "Payment initiated successfully for $months months")
                    } else {
                        _paymentState.value = PaymentState.ERROR
                        Log.e("VipViewModel", "Error initiating payment", payResult.exceptionOrNull())
                    }
                } else {
                    _paymentState.value = PaymentState.ERROR
                    Log.e("VipViewModel", "Error creating order", orderResult.exceptionOrNull())
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.ERROR
                Log.e("VipViewModel", "Error in purchaseVip", e)
            }
        }
    }

    fun setSelectedMonths(months: Int) {
        _selectedMonths.value = months
        Log.d("VipViewModel", "Selected months set to: $months")
    }
    
    fun setSelectedAmount(amount: Long) {
        _selectedAmount.value = amount
        Log.d("VipViewModel", "Selected amount set to: $amount")
    }
    
    fun setSelectedDescription(description: String) {
        _selectedDescription.value = description
        Log.d("VipViewModel", "Selected description set to: $description")
    }

    fun handleZaloPayResult(isSuccess: Boolean, months: Int = _selectedMonths.value) {
        viewModelScope.launch {
            Log.d("VipViewModel", "Handling ZaloPay result: isSuccess=$isSuccess, months=$months")
            
            if (isSuccess) {
                _paymentState.value = PaymentState.SUCCESS
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                
                if (userId != null) {
                    Log.d("VipViewModel", "Updating VIP status for user $userId with $months months")
                    
                    zaloPayRepository.updateVipStatus(userId, months)
                        .onSuccess {
                            Log.d("VipViewModel", "VIP status updated successfully")
                            checkVipStatus() // Refresh UI
                        }
                        .onFailure { error ->
                            Log.e("VipViewModel", "Failed to update VIP status", error)
                        }
                } else {
                    Log.e("VipViewModel", "User ID is null, cannot update VIP status")
                }
            } else {
                _paymentState.value = PaymentState.ERROR
                Log.d("VipViewModel", "Payment was not successful")
            }
        }
    }

    fun refreshVipStatus() {
        Log.d("VipViewModel", "Refreshing VIP status")
        checkVipStatus()
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.IDLE
    }

    // Thêm hàm cập nhật trạng thái thanh toán từ ngoài (nếu cần)
    fun setPaymentState(state: PaymentState) {
        _paymentState.value = state
    }
}

enum class PaymentState {
    IDLE, LOADING, SUCCESS, ERROR, CANCELED
}









