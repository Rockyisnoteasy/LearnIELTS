// 统一词典播放与查询接口，实现缓存优先与语音生成,DictionaryViewModel调用


package com.example.learnielts.data.repository

import android.content.Context
import android.util.Log
import com.example.learnielts.utils.AudioPlayer
import com.example.learnielts.data.datasource.TTSDataSource

class DictRepository(
    private val context: Context,
    private val ttsSource: TTSDataSource
) {
    suspend fun playWordAudio(word: String, speed: Float = 1.0f) {
        Log.d("调试", "DictRepository 接收到播放请求：$word")

        val cachedFile = com.example.learnielts.utils.VoiceCacheManager.getVoicePathIfExists(word, context)

        if (cachedFile != null) {
            Log.d("调试", "找到缓存路径，准备播放：${cachedFile.absolutePath}")
            AudioPlayer.play(context, cachedFile)
            return
        }

        Log.d("调试", "未找到缓存，准备调用 downloadAudio")
        val downloaded = ttsSource.downloadAudio(word, speed)

        if (downloaded == null) {
            Log.e("调试", "下载失败，播放终止")
            return
        }

        Log.d("调试", "下载完成，准备播放：${downloaded.absolutePath}")
        AudioPlayer.play(context, downloaded)
    }


}