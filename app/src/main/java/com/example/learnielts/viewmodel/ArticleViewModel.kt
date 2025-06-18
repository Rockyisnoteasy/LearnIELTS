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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import com.example.learnielts.utils.VoiceCacheManager


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

    // 用于存放“每日阅读”的最新文章
    private val _latestArticle = MutableStateFlow<ArticleSnippet?>(null)
    val latestArticle: StateFlow<ArticleSnippet?> = _latestArticle.asStateFlow()


    init {
        // 收集 token，并在 token 变化时触发文章列表加载
        viewModelScope.launch {
            authViewModel.token.collect { token ->
                if (token != null) {
                    fetchArticleList()
                    fetchLatestArticle() // 自动登录或token变化时也获取最新文章
                } else {
                    _articleList.value = emptyList() // 用户登出时清空列表
                    _currentArticle.value = null
                    _latestArticle.value = null
                    _isFavorite.value = false
                    _userNote.value = null
                }
            }
        }
    }

    fun fetchLatestArticle() {
        viewModelScope.launch {
            _isLoading.value = true
            val token = authViewModel.token.value ?: run {
                _isLoading.value = false
                return@launch
            }
            try {
                // 新增 articleType = "daily" 参数
                val articles = articleRepository.getArticleList(token, page = 1, limit = 1, articleType = "daily")
                _latestArticle.value = articles.firstOrNull()
                Log.d("调试", "Fetched latest article: ${_latestArticle.value?.title}")
            } catch (e: Exception) {
                Log.e("调试", "Error fetching latest article", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchArticleList(page: Int = 1, limit: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val token = authViewModel.token.value
            if (token == null) {
                _errorMessage.value = "用户未登录，无法获取文章列表"
                _isLoading.value = false
                return@launch
            }
            try {
                // ✅ 新增 articleType = "others" 参数
                val articles = articleRepository.getArticleList(token, page, limit, articleType = "others")
                _articleList.value = articles
                Log.d("调试", "Fetched ${articles.size} 'other' articles.")
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

    // 在 ArticleViewModel 类的内部

    /**
     * 预缓存文章内所有句子的音频文件。
     * @param articleDetail 包含句子列表的文章详情对象。
     */
    fun precacheArticleAudio(articleDetail: ArticleDetail) {
        viewModelScope.launch(Dispatchers.IO) { // 在后台 IO 线程执行
            Log.d("调试", "【音频预缓存】开始为文章 '${articleDetail.title}' 预缓存音频...")

            val voiceCacheDir = VoiceCacheManager.getVoiceCacheDir(getApplication())
            if (!voiceCacheDir.exists()) {
                voiceCacheDir.mkdirs()
            }
            val client = OkHttpClient()

            // 按顺序遍历文章中的每一个句子
            for ((index, sentence) in articleDetail.content.withIndex()) {
                // 确保句子有音频URL
                sentence.audioUrl?.let { url ->
                    // 使用和详情页一致的MD5算法生成唯一文件名
                    val fileName = md5(url) + ".mp3"
                    val localFile = File(voiceCacheDir, fileName)

                    // 如果文件不存在，则下载
                    if (!localFile.exists()) {
                        try {
                            Log.d("调试", "【音频预缓存】缓存第 ${index + 1} 句: $url")
                            val request = Request.Builder().url(url).build()
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { input ->
                                    localFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                Log.d("调试", "【音频预缓存】✅ 第 ${index + 1} 句缓存成功 -> ${localFile.name}")
                            } else {
                                Log.e("调试", "【音频预缓存】❌ 第 ${index + 1} 句下载失败，响应码: ${response.code}")
                            }
                        } catch (e: Exception) {
                            Log.e("调试", "【音频预缓存】❌ 第 ${index + 1} 句缓存异常: ${e.message}")
                        }
                    } else {
                        Log.d("调试", "【音频预缓存】⏩ 第 ${index + 1} 句已存在，跳过。")
                    }
                }
            }
            Log.d("调试", "【音频预缓存】文章 '${articleDetail.title}' 的所有音频已处理完毕。")
        }
    }

    // MD5 函数，如果 ViewModel 中没有，可以添加
    private fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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