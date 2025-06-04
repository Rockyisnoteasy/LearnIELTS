// 统一管理本地缓存音频文件与 wordbook_android.json,DictRepository、AudioPlayer调用
package com.example.learnielts.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object VoiceCacheManager {
    private const val FILE_NAME = "wordbook_android.json"

    fun getVoiceCacheDir(context: Context): File {
        return File(context.getExternalFilesDir(null), "voice_cache")
    }

    fun getMappingFile(context: Context): File {
        return File(context.getExternalFilesDir(null), FILE_NAME)
    }

    fun loadMapping(context: Context): MutableMap<String, String> {
        val externalMap = try {
            val extFile = File(context.getExternalFilesDir(null), FILE_NAME)
            if (extFile.exists()) {
                val json = JSONObject(extFile.readText())
                json.keys().asSequence().associateWith { json.getString(it) }
            } else emptyMap()
        } catch (e: Exception) {
            Log.e("调试", "读取外部 wordbook 失败: ${e.message}")
            emptyMap()
        }

        val internalMap = try {
            val intFile = File(context.filesDir, FILE_NAME)
            if (intFile.exists()) {
                val json = JSONObject(intFile.readText())
                json.keys().asSequence().associateWith { json.getString(it) }
            } else emptyMap()
        } catch (e: Exception) {
            Log.e("调试", "读取内部 wordbook 失败: ${e.message}")
            emptyMap()
        }

        // ✅ 外部优先，如果 key 重复，以外部为准
        return (internalMap + externalMap).toMutableMap()
    }


    fun saveMapping(context: Context, data: Map<String, String>) {
        try {
            val json = JSONObject(data)
            File(context.filesDir, FILE_NAME).writeText(json.toString(2))
            Log.d("调试", "✅ 写入映射文件成功")
        } catch (e: Exception) {
            Log.e("调试", "保存 wordbook.json 失败: ${e.message}")
        }
    }


    fun md5(text: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun getOrGenerateVoicePath(word: String, context: Context): File {
        val map = loadMapping(context)
        val fileName = "${md5(word)}.mp3"
        val voiceFile = File(getVoiceCacheDir(context), fileName)

        // ✅ 修改：只要映射缺失，就补写；不管文件是否存在
        if (!map.containsKey(word)) {
            map[word] = "voice_cache/$fileName"
            saveMapping(context, map)
            Log.d("调试", "✅ 写入映射：$word → voice_cache/$fileName")
        } else {
            Log.d("调试", "ℹ️ 映射已存在：$word → ${map[word]}")
        }

        return voiceFile
    }


    fun getVoicePathIfExists(word: String, context: Context): File? {
        val map = loadMapping(context)
        val relative = map[word] ?: return null
        val file = File(context.getExternalFilesDir(null), relative)

        Log.d("调试", "尝试查找缓存路径 for [$word] = $relative")
        Log.d("调试", "文件实际路径: ${file.absolutePath}")
        Log.d("调试", "文件是否存在: ${file.exists()}")

        return if (file.exists()) file else null
    }

}
