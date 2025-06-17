// learnielts/data/model/Article.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate // 使用 java.time.LocalDate 而不是 java.util.Date


// 对应后端 Sentence 模型
data class Sentence(
    val sentence: String, // 英文句子原文
    val translation: String, // 中文翻译
    @SerializedName("audio_url") val audioUrl: String? = null, // 句子音频 URL，后端可能返回空
    @SerializedName("is_heading") val isHeading: Boolean = false
)

// 对应后端文章列表的简略信息 (ArticleSnippet)
data class ArticleSnippet(
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    val summary: String? = null,
    @SerializedName("difficulty_level") val difficultyLevel: Int, // 难度级别
    val source: String? = null, // 文章来源，后端可能返回空
    @SerializedName("cover_image_url") val coverImageUrl: String? = null, // 封面图 URL，后端可能返回空
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("publish_date") val publishDate: LocalDate // 发布日期
)

// 对应后端文章详情 (ArticleDetail)
data class ArticleDetail(
    val id: Int,
    val title: String,
    val subtitle: String? = null,    // 新增
    val summary: String? = null,
    @SerializedName("difficulty_level") val difficultyLevel: Int,
    val source: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("publish_date") val publishDate: LocalDate,
    val content: List<Sentence> // 文章内容，由 Sentence 对象列表组成
)

// 用于收藏文章的请求体和响应
data class FavoriteArticleRequest(
    @SerializedName("article_id") val articleId: Int
)

// 用于笔记的请求体和响应
data class ArticleNoteRequest(
    @SerializedName("article_id") val articleId: Int,
    @SerializedName("note_content") val noteContent: String
)

data class ArticleNoteResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("article_id") val articleId: Int,
    @SerializedName("note_content") val noteContent: String,
    @SerializedName("last_updated") val lastUpdated: String // 或 LocalDate/LocalDateTime
)

data class BaseResponse( // 用于通用的成功/失败响应
    val message: String
)