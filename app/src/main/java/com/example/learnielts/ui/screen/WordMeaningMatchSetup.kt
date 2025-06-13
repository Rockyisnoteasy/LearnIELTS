// learnielts/ui/screen/WordMeaningMatchSetup.kt
package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.ui.screen.common.WordPlanSource
import com.example.learnielts.utils.ChineseDefinitionExtractor
import com.example.learnielts.viewmodel.DictionaryViewModel

@Composable
fun WordMeaningMatchSetup(
    context: Context,
    viewModel: DictionaryViewModel,
    planSource: WordPlanSource,
    selectedPlanName: String?,
    onBack: () -> Unit,
    onStartTest: (List<Pair<String, String>>) -> Unit
) {
    DateWordPickerScreen(
        context = context,
        title = "选择日期开始词意匹配",
        allowMultiSelect = true,
        source = planSource,
        selectedPlanName = selectedPlanName,
        onConfirm = { words ->
            val testItems = words.mapNotNull { word ->
                // 1. 获取完整释义
                val fullDef = viewModel.getDefinition(word)
                // 2. 初步简化
                val simplifiedDef = ChineseDefinitionExtractor.simplify(fullDef)
                // 3. 终极简化
                val ultraSimplifiedDef = ChineseDefinitionExtractor.ultraSimplify(simplifiedDef)

                if (ultraSimplifiedDef != null) {
                    word to ultraSimplifiedDef
                } else {
                    null
                }
            }.shuffled()

            if (testItems.isNotEmpty()) {
                onStartTest(testItems)
            }
        },
        onBack = onBack
    )
}