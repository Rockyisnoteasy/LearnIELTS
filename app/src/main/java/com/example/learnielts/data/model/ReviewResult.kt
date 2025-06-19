// 文件路径: learnielts/data/model/ReviewResult.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

data class ReviewResult(
    @SerializedName("word") val word: String,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("test_type") val testType: String // e.g., "low_selection", "high_recall"
)