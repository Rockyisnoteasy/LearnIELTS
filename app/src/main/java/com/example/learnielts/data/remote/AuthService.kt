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

import com.example.learnielts.data.model.ArticleSnippet
import com.example.learnielts.data.model.ArticleDetail
import com.example.learnielts.data.model.FavoriteArticleRequest
import com.example.learnielts.data.model.ArticleNoteRequest
import com.example.learnielts.data.model.ArticleNoteResponse
import com.example.learnielts.data.model.BaseResponse

// 定义请求体
data class SentenceReviewRequest(val word: String, val sentence: String)

// 定义响应体
data class SentenceReviewResponse(val feedback: String)

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

    // 以下是为学习计划同步新增的接口 ---

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

    // 以词造句审核接口
    @POST("judge-sentence")
    suspend fun reviewSentence(
        @Header("Authorization") authHeader: String,
        @Body data: SentenceReviewRequest
    ): SentenceReviewResponse

    // --- 以下是为阅读功能新增的接口 ---

    /**
     * 获取文章列表
     * @param page 页码，默认为1
     * @param limit 每页数量，默认为10
     */
    @GET("articles")
    suspend fun getArticleList(
        @Header("Authorization") authHeader: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("article_type") articleType: String? = null
    ): List<ArticleSnippet>

    /**
     * 获取单篇文章的详细内容
     * @param articleId 文章ID
     */
    @GET("articles/{article_id}")
    suspend fun getArticleDetail(
        @Header("Authorization") authHeader: String,
        @Path("article_id") articleId: Int
    ): ArticleDetail

    /**
     * 收藏文章
     * @param articleId 文章ID
     */
    @POST("articles/{article_id}/favorite")
    suspend fun addFavorite(
        @Header("Authorization") authHeader: String,
        @Path("article_id") articleId: Int
    ): BaseResponse

    /**
     * 取消收藏文章
     * @param articleId 文章ID
     */
    @DELETE("articles/{article_id}/favorite")
    suspend fun removeFavorite(
        @Header("Authorization") authHeader: String,
        @Path("article_id") articleId: Int
    ): BaseResponse

    /**
     * 获取当前用户收藏的所有文章列表
     */
    @GET("favorites") // 假设后端有一个 /favorites 接口获取用户所有收藏
    suspend fun getFavoriteArticles(
        @Header("Authorization") authHeader: String
    ): List<ArticleSnippet>

    /**
     * 添加或更新文章笔记
     * @param articleId 文章ID
     * @param data 包含笔记内容的请求体
     */
    @POST("articles/{article_id}/notes")
    suspend fun saveNote(
        @Header("Authorization") authHeader: String,
        @Path("article_id") articleId: Int,
        @Body data: ArticleNoteRequest
    ): ArticleNoteResponse // 后端可能返回更新后的笔记信息

    /**
     * 获取某篇文章的用户笔记
     * @param articleId 文章ID
     */
    @GET("articles/{article_id}/notes")
    suspend fun getNote(
        @Header("Authorization") authHeader: String,
        @Path("article_id") articleId: Int
    ): ArticleNoteResponse? // 可能没有笔记，返回 null


}
