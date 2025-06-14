// 本仓库类将负责协调数据来源（主要是 AuthService），处理文章相关的业务逻辑，并为 ViewModel 提供一个干净的 API。

// learnielts/data/repository/ArticleRepository.kt
package com.example.learnielts.data.repository

import android.util.Log
import com.example.learnielts.data.remote.AuthService
import com.example.learnielts.data.model.ArticleSnippet
import com.example.learnielts.data.model.ArticleDetail
import com.example.learnielts.data.model.ArticleNoteRequest
import com.example.learnielts.data.model.ArticleNoteResponse
import com.example.learnielts.data.model.FavoriteArticleRequest // 虽然请求体可能不需要显式传入，但为了保持一致性可以保留

class ArticleRepository(private val authService: AuthService) {

    // 获取文章列表
    suspend fun getArticleList(token: String, page: Int, limit: Int): List<ArticleSnippet> {
        return try {
            authService.getArticleList("Bearer $token", page, limit)
        } catch (e: Exception) {
            Log.e("调试", "Failed to get article list: ${e.message}", e)
            emptyList()
        }
    }

    // 获取单篇文章详情
    suspend fun getArticleDetail(token: String, articleId: Int): ArticleDetail? {
        return try {
            authService.getArticleDetail("Bearer $token", articleId)
        } catch (e: Exception) {
            Log.e("调试", "Failed to get article detail for ID $articleId: ${e.message}", e)
            null
        }
    }

    // 收藏文章
    suspend fun addFavorite(token: String, articleId: Int): Boolean {
        return try {
            // 后端 /articles/{article_id}/favorite 接口接受的是 Path 参数，而不是请求体
            val response = authService.addFavorite("Bearer $token", articleId)
            response.message == "Success" // 或者检查其他成功标识
        } catch (e: Exception) {
            Log.e("调试", "Failed to add favorite for article ID $articleId: ${e.message}", e)
            false
        }
    }

    // 取消收藏文章
    suspend fun removeFavorite(token: String, articleId: Int): Boolean {
        return try {
            val response = authService.removeFavorite("Bearer $token", articleId)
            response.message == "Success"
        } catch (e: Exception) {
            Log.e("调试", "Failed to remove favorite for article ID $articleId: ${e.message}", e)
            false
        }
    }

    // 获取用户收藏的文章列表
    suspend fun getFavoriteArticles(token: String): List<ArticleSnippet> {
        return try {
            authService.getFavoriteArticles("Bearer $token")
        } catch (e: Exception) {
            Log.e("调试", "Failed to get favorite articles: ${e.message}", e)
            emptyList()
        }
    }

    // 保存/更新笔记
    suspend fun saveNote(token: String, articleId: Int, noteContent: String): ArticleNoteResponse? {
        return try {
            authService.saveNote("Bearer $token", articleId, ArticleNoteRequest(articleId, noteContent))
        } catch (e: Exception) {
            Log.e("调试", "Failed to save note for article ID $articleId: ${e.message}", e)
            null
        }
    }

    // 获取笔记
    suspend fun getNote(token: String, articleId: Int): ArticleNoteResponse? {
        return try {
            authService.getNote("Bearer $token", articleId)
        } catch (e: Exception) {
            Log.e("调试", "Failed to get note for article ID $articleId: ${e.message}", e)
            null
        }
    }
}