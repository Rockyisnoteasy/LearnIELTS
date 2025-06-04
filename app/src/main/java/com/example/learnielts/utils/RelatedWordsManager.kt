// 从释义中识别变形词（如复数、过去式）并建立映射,DictRepository、WordListScreen调用
package com.example.learnielts.util

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

object RelatedWordsManager {

    private const val FILE_NAME = "related_words.json"

    private fun getJsonFile(context: Context): File {
        return File(context.getExternalFilesDir(null), FILE_NAME)
    }

    fun loadRelatedWords(context: Context): MutableMap<String, String> {
        val file = getJsonFile(context)
        if (!file.exists()) return mutableMapOf()
        return try {
            val json = JSONObject(file.readText())
            json.keys().asSequence().associateWith { json.getString(it) }.toMutableMap()
        } catch (e: Exception) {
            Log.e("调试", "读取失败: ${e.message}")
            mutableMapOf()
        }
    }

    fun saveRelatedWords(context: Context, data: Map<String, String>) {
        try {
            val json = JSONObject(data)
            val file = getJsonFile(context)
            file.parentFile?.mkdirs()
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e("调试", "保存失败: ${e.message}")
        }
    }

    fun extractAndSaveRelatedWords(word: String, definition: String, context: Context) {
        val patterns = listOf(
            "- 单数：(\\w+)",
            "- 复数：(\\w+)",
            "- 第三人称单数：(\\w+)",
            "- 三单：(\\w+)",
            "- 现在分词：(\\w+)",
            "- 过去式：(\\w+)",
            "- 过去分词：(\\w+)",
            "- 过去式/过去分词：(\\w+)"
        )

        val relatedMap = loadRelatedWords(context)
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(definition)
            while (matcher.find()) {
                val form = matcher.group(1).lowercase()
                if (!relatedMap.containsKey(form)) {
                    relatedMap[form] = word.lowercase()
                }
            }
        }

        saveRelatedWords(context, relatedMap)
    }

    fun getRealSourceWord(word: String, context: Context): String {
        val relatedMap = loadRelatedWords(context)
        return relatedMap[word.lowercase()] ?: word
    }
}
