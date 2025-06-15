//听力测试模块设置页，选择测试词汇的日期,MainActivity调用
package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.ui.screen.common.WordPlanSource
import androidx.activity.compose.BackHandler

@Composable
fun ListeningTestSetupScreen(
    context: Context,
    planSource: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onStartTest: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }
    DateWordPickerScreen(
        context = context,
        title = "选择日期进入听力填空",
        allowMultiSelect = true,
        source = planSource,
        selectedPlanName = selectedPlanName,
        onConfirm = onStartTest,
        onBack = onBack
    )
}


