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

    private const val API_KEY = "sk-tjjkdkomeoqcupwrizljtdarajgdakuvkbjytjitygtokibu"
    private const val API_URL = "https://api.siliconflow.cn/v1/chat/completions"
    private const val MODEL = "Pro/THUDM/glm-4-9b-chat"

    suspend fun judgeSentence(word: String, sentence: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(word, sentence)

            val json = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "❌ 请求失败：${response.code} ${response.message}"
            }

            val result = JSONObject(response.body?.string() ?: "")
            val reply = result
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            return@withContext reply.trim()

        } catch (e: Exception) {
            Log.e("调试", "调用异常：${e.message}")
            return@withContext "❌ 调用 AI 服务失败：${e.message}"
        }
    }

    private fun buildPrompt(word: String, sentence: String): String {
        return """
        你是一位资深的英文写作教师。请围绕用户提交的句子，判断其是否正确使用了英文单词 “$word”。

        - 请首先分析该句子的语法是否正确，是否符合该单词的使用习惯；
        - 如果用户用错了，也请坚持围绕该单词给出至少一个正确用法的参考句子（不要完全换掉该单词）；
        - 如果用户写对了，也请补充一个同样含有该单词的例句以加强记忆。

        句子：$sentence
        """.trimIndent()
    }
}
