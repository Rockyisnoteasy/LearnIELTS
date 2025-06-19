// 文件路径: learnielts/data/model/SubmitReviewRequest.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

data class SubmitReviewRequest(
    @SerializedName("review_batch_id") val reviewBatchId: String,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("results") val results: List<ReviewResult>
)