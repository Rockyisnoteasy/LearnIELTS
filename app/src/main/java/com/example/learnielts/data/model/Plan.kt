package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用于创建新学习计划的请求体
 */
data class PlanCreateRequest(
    @SerializedName("plan_name") val planName: String,
    val category: String,
    @SerializedName("selected_plan") val selectedPlan: String,
    @SerializedName("daily_count") val dailyCount: Int
)

/**
 * 每日单词列表的数据结构，用于和服务器同步
 */
data class DailyWords(
    @SerializedName("word_date") val wordDate: String, // e.g., "2025-06-12"
    val words: List<String>
)

/**
 * 从服务器获取的完整学习计划响应体
 */
data class PlanResponse(
    val id: Int,
    @SerializedName("plan_name") val planName: String,
    val category: String,
    @SerializedName("selected_plan") val selectedPlan: String,
    @SerializedName("daily_count") val dailyCount: Int,
    @SerializedName("daily_words") val dailyWords: List<DailyWords>
)