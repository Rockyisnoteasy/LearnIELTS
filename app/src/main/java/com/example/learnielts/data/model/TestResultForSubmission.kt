// 文件路径: learnielts/data/model/TestResultForSubmission.kt
package com.example.learnielts.data.model

// 这个数据类用于在客户端各模块之间传递标准化的测试结果
data class TestResultForSubmission(
    val word: String,
    val isCorrect: Boolean
)