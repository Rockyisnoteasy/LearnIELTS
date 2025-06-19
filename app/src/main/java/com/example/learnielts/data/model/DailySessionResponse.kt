// 文件路径: learnielts/data/model/DailySessionResponse.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

data class DailySessionResponse(
    @SerializedName("review_words") val reviewWords: List<String>,
    @SerializedName("unmastered_word_count") val unmasteredWordCount: Int,
    @SerializedName("is_new_word_paused") val isNewWordPaused: Boolean
)