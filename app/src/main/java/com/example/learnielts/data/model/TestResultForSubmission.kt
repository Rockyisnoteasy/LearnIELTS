// 文件路径: learnielts/data/model/TestResultForSubmission.kt
package com.example.learnielts.data.model

// 这个数据类用于在客户端各模块之间传递标准化的测试结果
data class TestResultForSubmission(
    val word: String,
    val isCorrect: Boolean
)

data class SpeechRecognitionResponse(
    val recognized_text: String,
    val is_correct: Boolean,
    val correct_word: String,
    val message: String
)

// Pydantic model for the request to generate an upload URL
data class GenerateUploadUrlRequest(
    val filename: String
)

// Pydantic model for the response containing the signed URL
data class GenerateUploadUrlResponse(
    val upload_url: String,
    val object_key: String
)

// Pydantic model for the request to trigger recognition from an OSS file
data class SubmitOssForRecognitionRequest(
    val object_key: String,
    val word: String,
    val plan_id: Int
)