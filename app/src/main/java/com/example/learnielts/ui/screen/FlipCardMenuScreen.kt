package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.ui.screen.common.WordPlanSource


@Composable
fun FlipCardMenuScreen(
    context: Context,
    planSource: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onStartTest: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    DateWordPickerScreen(
        context = context,
        title = "选择日期进入翻牌记忆",
        allowMultiSelect = true,
        source = planSource,
        selectedPlanName = selectedPlanName,
        onConfirm = onStartTest,
        onBack = onBack
    )
}


