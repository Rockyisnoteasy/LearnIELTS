/**
 * User.kt
 *
 * 定义与用户认证相关的所有数据结构模型（data class）：
 *
 * ✅ LoginRequest：用于向后端发送登录请求，包含 username 和 password；
 * ✅ RegisterRequest：用于向后端发送注册请求，包含 email 和 password；
 * ✅ TokenResponse：登录成功后后端返回的 access_token 和 token_type；
 * ✅ UserProfile：通过 /profile 接口获取的用户资料，包含 id 和 email。
 */


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

data class SessionStatus(val ok: Boolean)

