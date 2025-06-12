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
 * ✅ 新增：与学习计划相关的增删查改接口
 */


package com.example.learnielts.data.remote

import com.example.learnielts.data.model.TokenResponse
import com.example.learnielts.data.model.UserProfile
import retrofit2.Response
import com.example.learnielts.data.model.RegisterRequest
import retrofit2.http.*
import com.example.learnielts.data.model.SessionStatus
import com.example.learnielts.data.model.DailyWords
import com.example.learnielts.data.model.PlanCreateRequest
import com.example.learnielts.data.model.PlanResponse


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

    @GET("is_current_session")
    suspend fun checkSession(@Header("Authorization") auth: String): SessionStatus

    // --- ✅ 以下是为学习计划同步新增的接口 ---

    /**
     * 获取当前用户的所有学习计划
     */
    @GET("plans")
    suspend fun getPlans(
        @Header("Authorization") authHeader: String
    ): List<PlanResponse>

    /**
     * 创建一个新的学习计划
     */
    @POST("plans")
    suspend fun createPlan(
        @Header("Authorization") authHeader: String,
        @Body planData: PlanCreateRequest
    ): PlanResponse

    /**
     * 为指定计划上传每日单词列表
     */
    @POST("plans/{plan_id}/daily_words")
    suspend fun addDailyWords(
        @Header("Authorization") authHeader: String,
        @Path("plan_id") planId: Int,
        @Body dailyWords: DailyWords
    ): Response<Unit>

    /**
     * 删除一个学习计划
     */
    @DELETE("plans/{plan_id}")
    suspend fun deletePlan(
        @Header("Authorization") authHeader: String,
        @Path("plan_id") planId: Int
    ): Response<Unit>

}
