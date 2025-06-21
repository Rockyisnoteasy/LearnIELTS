// learnielts/utils/AudioRecorder.kt
package com.example.learnielts.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    // 获取录音文件保存的目录
    private fun getRecordingDir(): File {
        val dir = File(context.externalCacheDir, "audio_recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 开始录音。
     * @return 录音文件将保存到的文件对象，如果无法开始录音则返回 null。
     */
    fun startRecording(): File? {
        // 确保上次的录音器已释放
        stopRecording()

        try {
            // ✅ 修改：保存为 .wav 格式
            outputFile = File(getRecordingDir(), "recording_${System.currentTimeMillis()}.amr")
            Log.d("调试", "准备录音到: ${outputFile?.absolutePath}")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // ✅ 2. 修改输出格式为 AMR_WB (宽带自适应多速率编码，非常适合语音)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)

                setAudioChannels(1)
                setAudioSamplingRate(16000) // 16000Hz 对于语音识别是足够的
                setAudioEncodingBitRate(96000) // 96kbps
                setOutputFile(outputFile?.absolutePath)

                try {
                    prepare()
                    start()
                    Log.d("调试", "录音开始")
                    return outputFile
                } catch (e: IOException) {
                    Log.e("调试", "录音准备或开始失败: ${e.message}", e)
                    releaseRecorder()
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e("调试", "创建 MediaRecorder 失败: ${e.message}", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * 停止录音并释放资源。
     * @return 录音文件对象，如果录音未开始或出错则返回 null。
     */
    fun stopRecording(): File? {
        val recordedFile = outputFile
        mediaRecorder?.apply {
            try {
                stop()
                release()
                Log.d("调试", "录音停止并释放，文件: ${recordedFile?.absolutePath}")
            } catch (e: RuntimeException) {
                Log.e("调试", "停止录音失败或非法状态: ${e.message}", e)
                recordedFile?.delete()
            } finally {
                mediaRecorder = null
                outputFile = null
            }
        } ?: run {
            Log.w("调试", "MediaRecorder 为空，无法停止。")
        }
        return recordedFile
    }

    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
        outputFile = null
    }

    /**
     * 删除指定的录音文件。
     */
    fun deleteRecording(file: File?) {
        file?.let {
            if (it.exists() && it.delete()) {
                Log.d("调试", "已删除录音文件: ${it.absolutePath}")
            } else {
                Log.e("调试", "删除录音文件失败或文件不存在: ${it?.absolutePath}")
            }
        }
    }
}