package com.example.learnielts.data.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

data class UserProfile(
    val id: Int,
    val email: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)
