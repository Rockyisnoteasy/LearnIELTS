// learnielts/ui/screen/SpeechRecognitionTestScreen.kt
package com.example.learnielts.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.learnielts.data.model.TestResultForSubmission
import com.example.learnielts.utils.AudioRecorder
import com.example.learnielts.utils.ChineseDefinitionExtractor
import com.example.learnielts.viewmodel.AuthViewModel
import com.example.learnielts.viewmodel.DictionaryViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import com.example.learnielts.data.model.SubmitReviewRequest
import com.example.learnielts.data.model.MasteredWordsResponse
import com.example.learnielts.data.model.GenerateUploadUrlResponse
import com.example.learnielts.data.model.GenerateUploadUrlRequest
import com.example.learnielts.data.model.SubmitOssForRecognitionRequest
import com.example.learnielts.data.model.SpeechRecognitionResponse
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import okhttp3.RequestBody.Companion.toRequestBody






@Composable
fun SpeechRecognitionTestScreen(
    context: Context,
    questions: List<Pair<String, String>>, // Pair<word, chinese meaning>
    viewModel: DictionaryViewModel,
    authViewModel: AuthViewModel, // 用于提交结果
    planId: Int?, // 传入当前学习计划的ID，用于提交熟练度结果
    onFinish: (results: List<Quad>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var currentIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<Quad>() }
    val coroutineScope = rememberCoroutineScope()
    val audioRecorder = remember { AudioRecorder(context) }
    val okHttpClient = remember { OkHttpClient() }

    var isRecording by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var isCurrentAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var loadingStatus by remember { mutableStateOf<String?>(null) }

    val currentWord = questions.getOrNull(currentIndex)?.first ?: return onFinish(results)
    val chineseMeaning = ChineseDefinitionExtractor.extract(viewModel.getDefinition(currentWord)) ?: "无中文释义"

    // 将所有函数定义在 Composable 顶部
    // ✅ ================== 函数定义区开始 ==================
    val startRecordingInternal = {
        if (!isRecording) {
            val audioFile = audioRecorder.startRecording()
            if (audioFile != null) {
                isRecording = true
                recognizedText = null
                isCurrentAnswerCorrect = null
                loadingStatus = null
                Log.d("调试", "开始录音...")
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecordingInternal()
        } else {
            Log.e("调试", "录音权限被拒绝")
            loadingStatus = "❌ 录音权限被拒绝"
        }
    }

    val checkAndRequestPermission = {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecordingInternal()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val stopRecordingAndSubmit = stop@{
        if (isRecording) {
            val audioFile = audioRecorder.stopRecording()
            isRecording = false

            if (audioFile == null) {
                loadingStatus = "❌ 录音失败，无法获取文件"
                Log.e("调试", "audioRecorder.stopRecording() 返回 null")
                return@stop
            }

            Log.d("调试", "停止录音，文件路径: ${audioFile.absolutePath}")

            coroutineScope.launch {
                try {
                    // ✅ 1. 确保文件写入完成
                    // 在读取文件用于上传前，加入一个极短的延迟，确保系统已将所有缓冲写入磁盘。
                    delay(200)

                    // ✅ 2. 获取上传签名URL
                    loadingStatus = "获取上传授权..."
                    val uploadInfo = authViewModel.generateUploadUrl(audioFile.name)
                    if (uploadInfo == null) {
                        loadingStatus = "❌ 获取上传授权失败"
                        audioRecorder.deleteRecording(audioFile)
                        return@launch
                    }

                    // ✅ 3. 执行上传，并等待其完成
                    // 使用 withContext(Dispatchers.IO) 确保网络操作在后台线程执行
                    val uploadSuccess = withContext(Dispatchers.IO) {
                        try {
                            // ✅ 每次上传都创建一个全新的 OkHttpClient 实例，避免状态残留
                            val uploaderClient = OkHttpClient()

                            loadingStatus = "上传录音中..."
                            val requestBody = audioFile.asRequestBody("audio/amr".toMediaTypeOrNull())
                            val putRequest = Request.Builder()
                                .url(uploadInfo.upload_url)
                                .put(requestBody)
                                .header("Content-Type", "audio/amr") // 确保Content-Type正确
                                .build()

                            // 使用 .execute() 执行同步调用，并等待结果
                            val response = uploaderClient.newCall(putRequest).execute()

                            if (response.isSuccessful) {
                                Log.d("调试", "✅ 录音成功上传到 OSS")
                                true // 返回上传成功
                            } else {
                                Log.e("调试", "OSS Upload failed: ${response.code} ${response.message} - ${response.body?.string()}")
                                false // 返回上传失败
                            }
                        } catch (e: Exception) {
                            Log.e("调试", "上传到OSS时发生IO异常", e)
                            false
                        }
                    }

                    // ✅ 4. 根据上传结果决定下一步
                    if (!uploadSuccess) {
                        loadingStatus = "❌ 上传录音失败"
                        audioRecorder.deleteRecording(audioFile) // 清理本地文件
                        return@launch
                    }

                    // 上传成功后可以安全地删除本地临时文件
                    audioRecorder.deleteRecording(audioFile)

                    // ✅ 5. 只有在上传成功后，才通知后端进行识别
                    loadingStatus = "识别中..."
                    val planIdForSubmission = planId ?: run {
                        loadingStatus = "❌ 无法获取学习计划ID"
                        return@launch
                    }
                    val recognitionResponse = authViewModel.submitOssForRecognition(
                        uploadInfo.object_key,
                        currentWord,
                        planIdForSubmission
                    )

                    if (recognitionResponse == null) {
                        loadingStatus = "❌ 识别服务调用失败"
                        return@launch
                    }

                    // ✅ 6. 更新UI
                    loadingStatus = null
                    recognizedText = recognitionResponse.recognized_text
                    isCurrentAnswerCorrect = recognitionResponse.is_correct

                    results.add(
                        Quad(
                            word = currentWord,
                            chinese = chineseMeaning,
                            userAnswer = recognitionResponse.recognized_text,
                            correct = recognitionResponse.is_correct
                        )
                    )
                    Log.d("调试", "识别结果: ${recognitionResponse.recognized_text}, 正确: ${recognitionResponse.is_correct}")

                } catch (e: Exception) {
                    Log.e("调试", "语音处理全流程失败", e)
                    loadingStatus = "❌ 处理失败: ${e.message}"
                } finally {
                    // 确保即使发生异常，本地文件也被尝试删除
                    if (audioFile.exists()) {
                        audioRecorder.deleteRecording(audioFile)
                    }
                }
            }
        }
    }


    LaunchedEffect(currentIndex) {
        recognizedText = null
        isCurrentAnswerCorrect = null
        loadingStatus = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("← 返回")
        }
        Spacer(Modifier.height(16.dp))

        Text("读词填空", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("中文释义：", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = chineseMeaning, style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(32.dp))

        // 录音按钮
        Button(
            onClick = {
                if (isRecording) {
                    stopRecordingAndSubmit()
                } else {
                    checkAndRequestPermission()
                }
            },
            modifier = Modifier.size(100.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isRecording) "停止录音" else "开始录音",
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(if (isRecording) "正在录音..." else "点击麦克风开始录音")

        Spacer(Modifier.height(24.dp))

        // 加载和结果显示区域
        if (loadingStatus != null) {
            if (loadingStatus!!.contains("...")) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
            }
            Text(loadingStatus!!)
        } else {
            recognizedText?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (isCurrentAnswerCorrect) {
                            true -> Color.Green.copy(alpha = 0.2f)
                            false -> Color.Red.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("你说了：", style = MaterialTheme.typography.bodyMedium)
                        Text(it, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("正确答案：", style = MaterialTheme.typography.bodyMedium)
                        Text(currentWord, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        currentIndex++
                        if (currentIndex >= questions.size) {
                            onFinish(results)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下一题")
                }
            }
        }
    }
}