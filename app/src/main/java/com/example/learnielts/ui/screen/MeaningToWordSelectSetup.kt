package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.utils.ChineseDefinitionExtractor
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.ui.screen.common.WordPlanSource

@Composable
fun MeaningToWordSelectSetup(
    context: Context,
    viewModel: DictionaryViewModel,
    planSource: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onBack: () -> Unit,
    onStartTest: (List<Pair<String, String>>) -> Unit = {}
) {
    DateWordPickerScreen(
        context = context,
        title = "选择日期开始以意选词",
        allowMultiSelect = true,
        source = planSource,
        selectedPlanName = selectedPlanName,
        onConfirm = { words ->
            val testItems = words.mapNotNull { word ->
                val fullDef = viewModel.getDefinition(word)
                val chinese = ChineseDefinitionExtractor.extract(fullDef)
                if (chinese != null) word to chinese else null
            }.shuffled()

            if (testItems.isNotEmpty()) {
                onStartTest(testItems)
            }
        },
        onBack = onBack
    )
}
