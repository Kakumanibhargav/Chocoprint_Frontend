package com.simats.chacolateprinter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.chacolateprinter.api.RetrofitClient
import com.simats.chacolateprinter.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _user = MutableStateFlow<UserDto?>(null)
    val user = _user.asStateFlow()

    private val _resetToken = MutableStateFlow<String?>(null)
    val resetToken = _resetToken.asStateFlow()

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun signup(fullName: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.instance.signup(
                    SignupRequest(fullName, email, password, confirmPassword)
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    _authState.value = AuthState.Success("Account created successfully")
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Signup failed"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.status == "success") {
                    _user.value = response.body()?.user
                    _authState.value = AuthState.Success("Login successful")
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Login failed"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.instance.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body()?.status == "success") {
                    _authState.value = AuthState.Success("OTP sent to your email")
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to send OTP"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.instance.verifyOtp(VerifyOtpRequest(email, otp))
                if (response.isSuccessful && response.body()?.status == "success") {
                    _resetToken.value = response.body()?.resetToken
                    _authState.value = AuthState.Success("OTP verified")
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Invalid OTP"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun resetPassword(newPassword: String, confirmPassword: String) {
        val token = _resetToken.value ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.instance.resetPassword(
                    ResetPasswordRequest(token, newPassword, confirmPassword)
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    _authState.value = AuthState.Success("Password reset successful")
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Reset failed"
                    _authState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Connection error")
            }
        }
    }
}
