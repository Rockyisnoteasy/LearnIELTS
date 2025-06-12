// DictionarySearchBar.kt
// 统一封装搜索栏组件，供首页和词典查询共用,HomeScreen、DictionaryScreen调用
package com.example.learnielts.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.viewmodel.TTSProvider
import com.example.learnielts.utils.ChineseDefinitionExtractor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.KeyboardArrowDown


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySearchBar(viewModel: DictionaryViewModel) {
    var input by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val suggestions = remember { mutableStateListOf<Pair<String, String>>() }
    val showDropdown = remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDefinitionSheet by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeightDp: Dp = configuration.screenHeightDp.dp
    val highlightColor = Color.Blue
    val baseColor = Color.Black

    LaunchedEffect(input) {
        suggestions.clear()
        if (input.isNotBlank() && input.all { it.isLetter() } && showDropdown.value) {
            val keyword = input.trim()
            val all = viewModel.entries.value

            val exact = all.find { it.word.equals(keyword, ignoreCase = true) }
            val partial = all
                .filter { it.word.startsWith(keyword, ignoreCase = true) && it.word != exact?.word }
                .take(9)

            val result = buildList {
                if (exact != null) add(exact)
                addAll(partial)
            }

            suggestions += result.mapNotNull { entry ->
                val chinese = ChineseDefinitionExtractor.simplify(entry.definition)
                if (chinese != null) entry.word to chinese else null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
    ) {
        Column {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    showDropdown.value = true
                },
                placeholder = { Text("输入查询；右滑打开功能菜单") },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            viewModel.playWord(input, context)
                        }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "播放发音")
                        }

                        IconButton(onClick = {
                            val isChinese = input.any { it.toInt() > 127 }
                            definition = if (isChinese) {
                                val results = viewModel.queryByChineseKeyword(input)
                                if (results.isNotEmpty()) {
                                    "匹配到以下英文单词：\n" + results.joinToString("\n")
                                } else {
                                    "❌ 未找到匹配的英文单词"
                                }
                            } else {
                                viewModel.getDefinition(input)
                            }
                            showDefinitionSheet = true
                            showDropdown.value = false
                            suggestions.clear()
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "查询")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
                    .heightIn(max = 600.dp),
                shadowElevation = 8.dp,
                color = Color.White,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.forEach { (word, chinese) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    input = word
                                    definition = viewModel.getDefinition(word)
                                    showDefinitionSheet = true
                                    suggestions.clear()
                                    showDropdown.value = false
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(word, style = MaterialTheme.typography.titleMedium)
                            Text(
                                chinese,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (definition != null && showDefinitionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDefinitionSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color.Gray, shape = RoundedCornerShape(2.dp))
                        )
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeightDp * 0.8f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // 显示当前搜索的单词，增加字重
                        Text(
                            text = input,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), // 增加字重
                            color = Color.Black,
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 16.dp)
                        )

                        val def = definition ?: ""
                        val chineseDefinitionStart = def.indexOf("中文释义：")
                        val processedDefinition = if (chineseDefinitionStart != -1) {
                            def.substring(chineseDefinitionStart)
                        } else {
                            def
                        }

                        val highlightColor = Color.Blue
                        val baseColor = Color.Black

                        // 定义所有可能的标题
                        val allHeaders = listOf(
                            "中文释义：", "词性：", "名词变形：", "动词变形：",
                            "常见例句：", "常见短语与搭配：", "近义词：",
                            "常见错误：", "补充说明：", "近义词（Synonyms）："
                        )

                        // 创建自定义的 TextStyle，调整行高
                        val customBodyLarge = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.8f // 增加行高，例如从 1.5f 调整为 1.8f
                        )
                        // 小标题字号放大
                        val customTitleMedium = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.2f, // 小标题字号比bodyLarge大1.2倍
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 2.0f // 增加行高，例如从 1.8f 调整为 2.0f
                        )
                        // 为“常见例句”中的中文定义新的 TextStyle
                        val exampleChineseStyle = MaterialTheme.typography.bodySmall.copy( // 较小字号
                            color = Color.Gray, // 浅色
                            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.8f // 适当行高
                        )

                        // 分割处理后的定义为行
                        val lines = processedDefinition.lines()

                        // 用于记录当前行是否是小标题 (外部循环的当前头部)
                        var currentHeaderInScope: String? = null

                        // 逐行渲染内容
                        lines.forEachIndexed { index, line ->
                            val trimmedLine = line.trim()
                            var isHeader = false

                            // 检查当前行是否是小标题
                            for (header in allHeaders) {
                                if (trimmedLine.startsWith(header)) {
                                    // 仅当不是第一个标题 "中文释义："时才添加分隔符
                                    if (header != "中文释义：") {
                                        Spacer(Modifier.height(18.dp)) // 从 12.dp 增加到 18.dp，保持同步变大
                                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp) // 浅色分隔符
                                        Spacer(Modifier.height(18.dp)) // 从 12.dp 增加到 18.dp，保持同步变大
                                    }
                                    isHeader = true
                                    currentHeaderInScope = header // 更新当前所处的小标题
                                    break
                                }
                            }

                            // 判断是否包含中文字符的辅助函数，排除特定标点
                            fun containsActualChinese(text: String): Boolean {
                                return text.any { char ->
                                    val script = Character.UnicodeScript.of(char.toInt())
                                    script == Character.UnicodeScript.HAN // 仅判断是否为汉字脚本
                                    // 或者更宽泛一点，排除常见全角符号
                                    // && char != '—' && char != '：' && char != '，' // 排除你已知会导致问题的特定全角符号
                                }
                            }

                            // 英文例句行判断：以 "-" 开头，且当前在“常见例句：”段落内
                            val isEnglishExampleLine =
                                trimmedLine.startsWith("-") && currentHeaderInScope == "常见例句：" && !isHeader //

                            // 识别中文例句行：不以 "-" 开头（排除英文例句），但包含中文字符，且当前在“常见例句：”段落内，并且本身不是小标题
                            // 且当前行不是小标题，以避免中文小标题被误判为中文例句行
                            val isChineseExampleLine =
                                !trimmedLine.startsWith("-") && containsActualChinese(trimmedLine) && currentHeaderInScope == "常见例句：" && !isHeader // 修正判断逻辑


                            // 构建高亮文本
                            val annotatedText = buildAnnotatedString {
                                val keyword = input.trim()
                                var start = 0
                                // 对每一行也应用关键词高亮
                                while (start < trimmedLine.length) {
                                    val index =
                                        trimmedLine.indexOf(keyword, start, ignoreCase = true)
                                    if (index == -1) {
                                        append(trimmedLine.substring(start))
                                        break
                                    } else {
                                        append(trimmedLine.substring(start, index))
                                        withStyle(
                                            style = SpanStyle(
                                                color = highlightColor,
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                            )
                                        ) { // 小标题加粗
                                            append(
                                                trimmedLine.substring(
                                                    index,
                                                    index + keyword.length
                                                )
                                            )
                                        }
                                        start = index + keyword.length
                                    }
                                }
                            }

                            // 渲染 Text
                            Text(
                                text = annotatedText,
                                style = when {
                                    isHeader -> customTitleMedium.copy(fontWeight = FontWeight.Bold) // 小标题整体加粗
                                    isChineseExampleLine -> exampleChineseStyle // 常见例句中文样式
                                    else -> customBodyLarge
                                },
                                color = if (isChineseExampleLine) exampleChineseStyle.color else baseColor // 确保颜色也应用到中文例句
                            )

                            // 如果是小标题，在其下方添加一个空行
                            if (isHeader) {
                                Spacer(Modifier.height(12.dp)) // 小标题下方空行
                            }
                            // 如果是常见例句中的中文行，且不是最后一个中文行，则空一行
                            else if (isChineseExampleLine) {
                                // 检查下一行是否还属于当前例句的中文部分
                                val nextLineIndex = index + 1
                                val hasMoreChineseExamples = nextLineIndex < lines.size &&
                                        !lines[nextLineIndex].trim()
                                            .startsWith("-") && // 下一行不以'-'开头
                                        containsActualChinese(lines[nextLineIndex].trim()) && // 下一行包含实际中文字符
                                        currentHeaderInScope == "常见例句：" // 仍在常见例句段落

                                if (!hasMoreChineseExamples) { // 只有当没有更多中文例句时才添加空行，避免连续空行
                                    Spacer(Modifier.height(12.dp)) // 常见例句中文下方空行
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}






