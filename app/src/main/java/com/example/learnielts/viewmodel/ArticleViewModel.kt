// learnielts/viewmodel/ArticleViewModel.kt
package com.example.learnielts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnielts.data.repository.ArticleRepository
import com.example.learnielts.data.remote.ApiClient // 用于获取 AuthService 实例
import com.example.learnielts.data.model.ArticleSnippet
import com.example.learnielts.data.model.ArticleDetail
import com.example.learnielts.data.model.ArticleNoteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ArticleViewModel(
    application: Application,
    private val authViewModel: AuthViewModel // 接收外部传入的 AuthViewModel
) : AndroidViewModel(application) {

    private val articleRepository: ArticleRepository = ArticleRepository(ApiClient.authService)

    private val _articleList = MutableStateFlow<List<ArticleSnippet>>(emptyList())
    val articleList: StateFlow<List<ArticleSnippet>> = _articleList.asStateFlow()

    private val _currentArticle = MutableStateFlow<ArticleDetail?>(null)
    val currentArticle: StateFlow<ArticleDetail?> = _currentArticle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _userNote = MutableStateFlow<String?>(null)
    val userNote: StateFlow<String?> = _userNote.asStateFlow()


    init {
        // 收集 token，并在 token 变化时触发文章列表加载
        viewModelScope.launch {
            authViewModel.token.collect { token ->
                if (token != null) {
                    fetchArticleList()
                } else {
                    _articleList.value = emptyList() // 用户登出时清空列表
                    _currentArticle.value = null
                    _isFavorite.value = false
                    _userNote.value = null
                }
            }
        }
    }

    fun fetchArticleList(page: Int = 1, limit: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val token = authViewModel.token.value // 获取当前 token
            if (token == null) {
                _errorMessage.value = "用户未登录，无法获取文章列表"
                _isLoading.value = false
                return@launch
            }
            try {
                val articles = articleRepository.getArticleList(token, page, limit)
                _articleList.value = articles
                Log.d("调试", "Fetched ${articles.size} articles.")
            } catch (e: Exception) {
                _errorMessage.value = "加载文章列表失败: ${e.message}"
                Log.e("调试", "Error fetching article list", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchArticleDetail(articleId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _currentArticle.value = null // 清空旧数据
            _isFavorite.value = false // 重置收藏状态
            _userNote.value = null // 重置笔记
            val token = authViewModel.token.value
            if (token == null) {
                _errorMessage.value = "用户未登录，无法获取文章详情"
                _isLoading.value = false
                return@launch
            }
            try {
                val article = articleRepository.getArticleDetail(token, articleId)
                _currentArticle.value = article
                Log.d("调试", "Fetched article detail for ID: $articleId")

                // 检查收藏状态
                checkFavoriteStatus(token, articleId)
                // 获取用户笔记
                fetchUserNote(token, articleId)

            } catch (e: Exception) {
                _errorMessage.value = "加载文章详情失败: ${e.message}"
                Log.e("调试", "Error fetching article detail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkFavoriteStatus(token: String, articleId: Int) {
        viewModelScope.launch {
            try {
                val favorites = articleRepository.getFavoriteArticles(token)
                _isFavorite.value = favorites.any { it.id == articleId }
                Log.d("调试", "Favorite status for $articleId: ${_isFavorite.value}")
            } catch (e: Exception) {
                Log.e("调试", "Error checking favorite status", e)
            }
        }
    }

    fun toggleFavorite(articleId: Int) {
        viewModelScope.launch {
            val token = authViewModel.token.value
            if (token == null) {
                _errorMessage.value = "用户未登录，无法操作收藏"
                return@launch
            }

            _isLoading.value = true
            try {
                val success = if (_isFavorite.value) {
                    articleRepository.removeFavorite(token, articleId)
                } else {
                    articleRepository.addFavorite(token, articleId)
                }
                if (success) {
                    _isFavorite.value = !_isFavorite.value // 切换本地状态
                    Log.d("调试", "Favorite toggled successfully. New status: ${_isFavorite.value}")
                } else {
                    _errorMessage.value = "收藏操作失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "收藏操作异常: ${e.message}"
                Log.e("调试", "Error toggling favorite", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchUserNote(token: String, articleId: Int) {
        viewModelScope.launch {
            try {
                val note = articleRepository.getNote(token, articleId)
                _userNote.value = note?.noteContent
                Log.d("调试", "Fetched user note for $articleId: ${_userNote.value}")
            } catch (e: Exception) {
                Log.e("调试", "Error fetching user note", e)
                _userNote.value = null
            }
        }
    }

    fun saveUserNote(articleId: Int, noteContent: String) {
        viewModelScope.launch {
            val token = authViewModel.token.value
            if (token == null) {
                _errorMessage.value = "用户未登录，无法保存笔记"
                return@launch
            }

            _isLoading.value = true
            try {
                val response = articleRepository.saveNote(token, articleId, noteContent)
                if (response != null) {
                    _userNote.value = response.noteContent // 更新本地笔记内容
                    Log.d("调试", "User note saved successfully for article $articleId.")
                } else {
                    _errorMessage.value = "保存笔记失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "保存笔记异常: ${e.message}"
                Log.e("调试", "Error saving user note", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 清除错误信息
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}
class ArticleViewModelFactory(
    private val application: Application,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArticleViewModel(application, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}