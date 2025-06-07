/**
 * AuthService.kt
 *
 * 定义认证相关的 Retrofit 接口声明，包括：
 *
 * ✅ login()：以表单方式发送用户名和密码，返回 TokenResponse；
 * ✅ register()：以 JSON 结构发送注册请求（email + password）；
 * ✅ getProfile()：带 Authorization 头，获取当前用户信息；
 *
 * 接口路径均以后端 FastAPI 路由为准：/login、/register、/profile。
 * login 接口使用 @FormUrlEncoded，其余使用默认的 JSON 格式。
 */


package com.example.learnielts.data.remote

import com.example.learnielts.data.model.TokenResponse
import com.example.learnielts.data.model.UserProfile
import retrofit2.Response
import com.example.learnielts.data.model.RegisterRequest
import retrofit2.http.*

interface AuthService {

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String
    ): UserProfile

    @POST("register")
    suspend fun register(
        @Body data: RegisterRequest
    ): Response<Unit>

}
