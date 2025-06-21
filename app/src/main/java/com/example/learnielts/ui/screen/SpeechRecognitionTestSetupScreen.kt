// learnielts/ui/screen/SpeechRecognitionTestSetupScreen.kt
package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.ui.screen.common.WordPlanSource
import com.example.learnielts.utils.ChineseDefinitionExtractor // 导入 ChineseDefinitionExtractor

@Composable
fun SpeechRecognitionTestSetupScreen(
    context: Context,
    viewModel: DictionaryViewModel, // 需要 DictionaryViewModel 来获取中文释义
    planSource: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onStartTest: (List<Pair<String, String>>) -> Unit, // 这里传递的是 Pair<word, chineseMeaning>
    onBack: () -> Unit
) {
    DateWordPickerScreen(
        context = context,
        title = "选择日期开始读词填空",
        allowMultiSelect = true,
        source = planSource,
        selectedPlanName = selectedPlanName,
        onConfirm = { words ->
            val testItems = words.mapNotNull { word ->
                // 获取单词的完整释义
                val fullDef = viewModel.getDefinition(word)
                // 提取中文释义，这与“以意选词”模块的中文释义选取方式一致
                val chinese = ChineseDefinitionExtractor.extract(fullDef)
                if (chinese != null) word to chinese else null
            }.shuffled() // 打乱顺序

            if (testItems.isNotEmpty()) {
                onStartTest(testItems)
            }
        },
        onBack = onBack
    )
}