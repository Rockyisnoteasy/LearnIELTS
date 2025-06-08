// 统一词典播放与查询接口，实现缓存优先与语音生成,DictionaryViewModel调用


package com.example.learnielts.data.repository

import android.content.Context
import android.util.Log
import com.example.learnielts.utils.AudioPlayer
import com.example.learnielts.data.datasource.TTSDataSource
import com.example.learnielts.data.room.AppDatabase
import com.example.learnielts.data.room.DictionaryDao
import com.example.learnielts.utils.VoiceCacheManager

class DictRepository(
    private val context: Context,
    private val ttsSource: TTSDataSource
) {
    private val dao: DictionaryDao by lazy {
        AppDatabase.getInstance(context).dictionaryDao()
    }

    suspend fun playWordAudio(word: String, speed: Float) {
        VoiceCacheManager.getOrDownloadVoiceFile(context, word) { file ->
            if (file != null) {
                AudioPlayer.play(context, file)
            } else {
                Log.e("调试", "❌ 无法获取语音文件")
            }
        }
    }


    // ✅ 添加的数据库随机干扰词获取函数
    suspend fun getRandomDistractorWords(correct: String, count: Int = 3): List<String> {
        val words = dao.getRandomWordsExcluding(correct, count)
        Log.d("调试", "✅ 从数据库随机取词（排除=$correct）：$words")
        return words
    }

    suspend fun getRandomDistractorDefinitions(correctWord: String, count: Int = 3): List<String> {
        return dao.getRandomDefinitionsExcluding(correctWord, count)
    }

}
