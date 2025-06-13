// 以词造句功能，用户写句子，AI 模型给出语法/表达点评,MainActivity、AIWritingJudge调用

package com.example.learnielts.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.learnielts.utils.AIWritingJudge
import kotlinx.coroutines.launch
import com.example.learnielts.viewmodel.DictionaryViewModel
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.learnielts.ui.screen.common.DateWordPickerScreen
import com.example.learnielts.ui.screen.common.WordPlanSource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.viewmodel.AuthViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun WordSentencePage(
    context: Context,
    viewModel: DictionaryViewModel,
    planSource: WordPlanSource = WordPlanSource.LEARNED_WORDS,
    selectedPlanName: String? = null,
    onExit: () -> Unit,
    // ✅ 新增参数：允许外部直接传入单词列表
    initialWords: List<String>? = null
) {
    // ✅ 修正：根据 initialWords 是否为空，决定起始 stage
    var stage by remember {
        mutableStateOf(if (initialWords.isNullOrEmpty()) "picker" else "select_word")
    }
    // ✅ 修正：如果 initialWords 不为空，直接用它初始化 selectedWords
    var selectedWords by remember { mutableStateOf(initialWords ?: emptyList()) }
    val authViewModel: AuthViewModel = viewModel()
    val tokenState by authViewModel.token.collectAsState()
    var selectedWord by remember { mutableStateOf("") }
    var sentenceInput by remember { mutableStateOf("") }
    var aiFeedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    when (stage) {
        "picker" -> DateWordPickerScreen(
            context = context,
            title = "选择日期以获取单词",
            allowMultiSelect = true,
            source = planSource,
            selectedPlanName = selectedPlanName,
            onConfirm = {
                selectedWords = it
                stage = "select_word"
            },
            onBack = onExit
        )

        "select_word" -> Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text("点击选择一个单词进行造句", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(selectedWords) { word ->
                    Text(
                        text = word,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                selectedWord = word
                                sentenceInput = ""      // 重置输入框内容
                                aiFeedback = null       // 清空上一条 AI 反馈
                                stage = "write"
                            }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            // 如果是从测试序列进入，不显示“重新选择日期”按钮
            if (initialWords.isNullOrEmpty()) {
                Button(onClick = { stage = "picker" }) {
                    Text("← 重新选择日期")
                }
            } else {
                Button(onClick = onExit) {
                    Text("退出")
                }
            }
        }

        "write" -> Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)) {
            Text("请用单词 “$selectedWord” 造一个英文句子：", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = sentenceInput,
                onValueChange = { sentenceInput = it },
                label = { Text("你的英文句子") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                aiFeedback = "AI 正在分析，请稍等..."
                scope.launch {
                    // 安全地获取 token，如果 token 存在则调用接口
                    tokenState?.let { token ->
                        aiFeedback = AIWritingJudge.judgeSentence(selectedWord, sentenceInput, token)
                    } ?: run {
                        // 如果 token 不存在（用户未登录），给出提示
                        aiFeedback = "❌ 错误：用户未登录，无法审核。"
                    }
                }
            }) {
                Text("提交 AI 审核")
            }

            aiFeedback?.let {
                Spacer(Modifier.height(16.dp))
                Text("AI反馈：", style = MaterialTheme.typography.titleMedium)
                Text(it)
            }

            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { stage = "select_word" }) {
                    Text("← 返回词列表")
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onExit) {
                    Text("返回主菜单")
                }
            }
        }
    }
}