package com.simats.chacolateprinter.api

import com.simats.chacolateprinter.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("signup")
    suspend fun signup(@Body request: SignupRequest): Response<ApiResponse<UserDto>>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<UserDto>>

    @POST("forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Unit>>

    @POST("reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Unit>>
}
