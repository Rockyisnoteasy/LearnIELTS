// 播放本地语音缓存,DictRepository调用

package com.example.learnielts.utils

import android.content.Context
import android.media.MediaPlayer
import java.io.File

object AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, file: File) {
        try {
            android.util.Log.d("调试", "AudioPlayer.play() 被调用，接收文件名：${file.name}")
            if (!file.exists()) {
                android.util.Log.e("调试", "文件不存在：${file.absolutePath}")
                return
            }

            android.util.Log.d("调试", "准备播放文件：${file.absolutePath}")

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }

            android.util.Log.d("调试", "播放启动成功")

        } catch (e: Exception) {
            android.util.Log.e("调试", "播放异常：${e.message}")
            e.printStackTrace()
        }
    }

}
