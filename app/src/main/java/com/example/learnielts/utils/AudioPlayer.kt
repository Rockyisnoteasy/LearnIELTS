// 播放本地语音缓存,DictRepository调用

// learnielts/utils/AudioPlayer.kt

package com.example.learnielts.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

object AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, file: File) {
        try {
            Log.d("调试", "AudioPlayer.play() 被调用，接收文件名：${file.name}")
            if (!file.exists()) {
                Log.e("调试", "文件不存在：${file.absolutePath}")
                return
            }

            // 在播放新的之前，先停止并释放旧的
            stop()

            Log.d("调试", "准备播放文件：${file.absolutePath}")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                // 监听播放完成事件，完成后自动释放资源
                setOnCompletionListener {
                    Log.d("调试", "播放完成，自动释放资源。")
                    stop()
                }
            }
            Log.d("调试", "播放启动成功")

        } catch (e: Exception) {
            Log.e("调试", "播放异常：${e.message}")
            e.printStackTrace()
            stop() // 出错时也尝试释放资源
        }
    }

    /**
     * ✅ 新增：停止播放并释放 MediaPlayer 资源
     */
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d("调试", "MediaPlayer 已停止并释放。")
            }
        } catch (e: IllegalStateException) {
            // MediaPlayer 可能处于不正确的状态，忽略错误
            Log.w("调试", "尝试停止 MediaPlayer 时出错: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}
