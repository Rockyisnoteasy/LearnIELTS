// TTSDataSource.kt
// TTS 数据源接口定义,Google/Tencent TTS 实现
package com.example.learnielts.data.datasource

import java.io.File

interface TTSDataSource {
    fun getAudioFilePath(word: String, speed: Float): File
    fun isCached(word: String, speed: Float): Boolean
    suspend fun downloadAudio(word: String, speed: Float): File?
}
