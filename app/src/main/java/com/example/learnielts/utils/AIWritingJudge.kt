// 以词造句模块使用的 AI 审核调用类，连接 API 接口,WordSentencePage调用

package com.example.learnielts.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object AIWritingJudge {
    // 移除所有 API Key 和 URL

    suspend fun judgeSentence(word: String, sentence: String, token: String): String {
        return try {
            val requestData = com.example.learnielts.data.remote.SentenceReviewRequest(word, sentence)

            // 通过你的 ApiClient 调用你自己的后端
            val response = com.example.learnielts.data.remote.ApiClient.authService.reviewSentence(
                "Bearer $token",
                requestData
            )

            response.feedback

        } catch (e: Exception) {
            Log.e("调试", "调用后端审核接口异常：${e.message}")
            "❌ 调用审核服务失败：${e.message}"
        }
    }
}
