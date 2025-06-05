// learned word里面的文件，是当日单词列表，可以与pyhon互通
// csv字典文件放在assets文件夹，wordbook_android.json，也就是映射文件，放在files文件夹下面
// 语音缓存文件也放在files文件夹下面。
// 应用主入口，管理页面导航、状态变量（如是否展示首页、翻牌页等）
// 当选择单词播放语音时，会先检查/storage/emulated/0/Android/data/com.example.learnielts/files/voice_cache
// 下面是否有缓存，如果没有，就调用TTS API，并把语音缓存保留在/storage/emulated/0/Android/data/com.example.learnielts/files，
// 同时，把单词-语音映射关系写在/data/data/com.example.learnielts/files/wordbook_android.json


package com.example.learnielts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.learnielts.ui.theme.LearnIELTSTheme
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.example.learnielts.viewmodel.TTSProvider

import androidx.compose.foundation.clickable

import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack
import com.example.learnielts.ui.theme.DrawerText
import com.example.learnielts.ui.screen.WordListScreen
import com.example.learnielts.ui.screen.ChineseToEnglishSetup
import com.example.learnielts.ui.screen.ChineseToEnglishTest
import com.example.learnielts.ui.screen.TestResultScreen
import com.example.learnielts.ui.screen.ListeningTestSetupScreen
import com.example.learnielts.ui.screen.ListeningTestScreen
import com.example.learnielts.ui.screen.ListeningTestResultScreen
import com.example.learnielts.ui.screen.FlipCardScreen
import com.example.learnielts.ui.screen.Quad
import com.example.learnielts.ui.screen.FlipCardMenuScreen
import com.example.learnielts.ui.screen.WordSentencePage
import com.example.learnielts.ui.screen.LearningPlanScreen
import com.example.learnielts.ui.screen.HomeScreen
import com.example.learnielts.ui.screen.common.WordPlanSource
import com.example.learnielts.utils.FileHelper
import com.example.learnielts.ui.screen.ChineseToEnglishSelectSetup
import com.example.learnielts.ui.screen.ChineseToEnglishSelect
import com.example.learnielts.ui.screen.MeaningToWordSelect
import com.example.learnielts.ui.screen.MeaningToWordSelectSetup
import com.example.learnielts.ui.screen.WordToMeaningSelect
import com.example.learnielts.ui.screen.WordToMeaningSelectSetup
import com.example.learnielts.ui.screen.WordToMeaningSelect


enum class DrawerLevel {
    MAIN_MENU, // 一级菜单（最上层）➡ 功能菜单 / 自主学习计划
    PLAN_MENU, // 学习计划
    SELF_PLAN_MENU, // 自主学习计划
    PLAN_CARD_MENU,   // ✅ 新增：点击“记忆卡”后显示学习计划
    PLAN_LISTEN_MENU,
    PLAN_CHINESE_MENU_SELECT, // 中翻英选字母填词
    PLAN_CHINESE_MENU_SPELL, // 中翻英填词
    PLAN_SENTENCE_MENU,
    PLAN_MANAGE_MENU,
    PLAN_MANAGE_ACTIONS,
    PLAN_MEANING_SELECT,
    PLAN_SELF_MEANING_SELECT,
    PLAN_WORD_MEANING_SELECT,
    PLAN_SELF_WORD_MEANING_SELECT,
}

class MainActivity : ComponentActivity() {
    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LearnIELTSTheme {
                AppContent(viewModel = viewModel) // 改为使用 AppContent
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LearnIELTSTheme {
        Greeting("Android")
    }
}

@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel,
    onEnterLearningPlan: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // ✅ 调用封装组件
        com.example.learnielts.ui.component.DictionarySearchBar(viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onEnterLearningPlan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📘 进入学习计划")
        }
    }
}


@Composable
fun AppContent(viewModel: DictionaryViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerLevel by remember { mutableStateOf(DrawerLevel.MAIN_MENU) }
    var showWordList by remember { mutableStateOf(false) }
    var showChineseToEnglish by remember { mutableStateOf(false) }
    var chineseTestScreenState by remember { mutableStateOf("setup") }
    var testQuestions by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var testResults by remember { mutableStateOf(emptyList<Quad>()) }
    var showListeningTest by remember { mutableStateOf(false) }
    var listeningTestState by remember { mutableStateOf("setup") }
    var listeningWordList by remember { mutableStateOf(listOf<String>()) }
    var listeningResults by remember { mutableStateOf(listOf<Triple<String, String, Boolean>>()) }
    var showFlipCard by remember { mutableStateOf(false) }
    var flipCardWords by remember { mutableStateOf(listOf<String>()) }
    var flipCardState by remember { mutableStateOf("menu") } // or "card"
    var showWordSentencePage by remember { mutableStateOf(false) }
    var showLearningPlan by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<String?>(null) }
    var learningPlanTarget by remember { mutableStateOf<String?>(null) }
    var selectedPlanToManage by remember { mutableStateOf<String?>(null) }
    var showChineseToEnglishSelect by remember { mutableStateOf(false) }
    var chineseSelectState by remember { mutableStateOf("setup") }
    var chineseSelectScreenState by remember { mutableStateOf("setup") }
    var chineseSelectResults by remember { mutableStateOf(emptyList<Quad>()) }
    var showMeaningSelect by remember { mutableStateOf(false) }
    var meaningSelectScreenState by remember { mutableStateOf("setup") }
    var showWordToMeaningSelect by remember { mutableStateOf(false) }
    var wordToMeaningSelectScreenState by remember { mutableStateOf("setup") }





    // 自动重置菜单状态
    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen) {
            drawerLevel = DrawerLevel.MAIN_MENU
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(220.dp),
                drawerContainerColor = Color((0xFF95E1D3)) // 这里设置抽屉背景色
            ) {
                when (drawerLevel) {

                    DrawerLevel.PLAN_MENU -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerLevel = DrawerLevel.MAIN_MENU
                                }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("返回")
                        }

                        Divider()

                        DrawerText("记忆卡") {
                            drawerLevel = DrawerLevel.PLAN_CARD_MENU
                        }

                        DrawerText("以词选意") {
                            drawerLevel = DrawerLevel.PLAN_WORD_MEANING_SELECT
                        }

                        DrawerText("以意选词") {
                            drawerLevel = DrawerLevel.PLAN_MEANING_SELECT
                        }

                        DrawerText("选择填词") {
                            drawerLevel = DrawerLevel.PLAN_CHINESE_MENU_SELECT
                        }


                        DrawerText("拼写填词") {
                            drawerLevel = DrawerLevel.PLAN_CHINESE_MENU_SPELL
                        }

                        DrawerText("听力填空") {
                            drawerLevel = DrawerLevel.PLAN_LISTEN_MENU
                        }

                        DrawerText("以词造句") {
                            drawerLevel = DrawerLevel.PLAN_SENTENCE_MENU
                        }
                    }


                    DrawerLevel.MAIN_MENU -> {
                        DrawerText("功能菜单", modifier = Modifier.padding(16.dp))
                        Divider()

                        DrawerText(
                            text = "学习计划",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerLevel = DrawerLevel.PLAN_MENU
                                }
                                .padding(16.dp)
                        )

                        // ✅ 添加：从学习计划进入四个子功能



                        DrawerText(
                            text = "自订学习计划",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerLevel = DrawerLevel.SELF_PLAN_MENU
                                }
                                .padding(16.dp)
                        )

                        DrawerText(
                            text = "新增学习计划",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLearningPlan = true
                                    learningPlanTarget = null // 表示纯计划管理，不跳转测试
                                    scope.launch { drawerState.close() }
                                }
                                .padding(16.dp)
                        )

                        DrawerText(
                            text = "管理学习计划",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerLevel = DrawerLevel.PLAN_MANAGE_MENU
                                }
                                .padding(16.dp)
                        )


                    }


                    DrawerLevel.SELF_PLAN_MENU -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    drawerLevel = DrawerLevel.MAIN_MENU
                                }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("返回")
                        }

                        Divider()

                        DrawerText("自订单词日历") {
                            showWordList = true
                            scope.launch { drawerState.close() }
                        }

                        DrawerText("记忆卡") {
                            showFlipCard = true
                            scope.launch { drawerState.close() }
                        }

                        DrawerText("以词选意") {
                            learningPlanTarget = "word_meaning_select_self"
                            showWordToMeaningSelect = true
                            wordToMeaningSelectScreenState = "setup"
                            scope.launch { drawerState.close() }
                        }

                        DrawerText("以意选词") {
                            showMeaningSelect = true
                            scope.launch { drawerState.close() }
                        }


                        DrawerText("选择填词") {
                            chineseTestScreenState = "setup"
                            showChineseToEnglishSelect = true
                            scope.launch { drawerState.close() }
                        }


                        DrawerText("拼写填词") {
                            chineseTestScreenState = "setup"
                            showChineseToEnglish = true
                            scope.launch { drawerState.close() }
                        }

                        DrawerText("听力填空") {
                            showListeningTest = true
                            scope.launch { drawerState.close() }
                        }

                        DrawerText("以词造句") {
                            showWordSentencePage = true
                            scope.launch { drawerState.close() }
                        }
                    }

                    DrawerLevel.PLAN_CARD_MENU -> {
                        // 返回按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划")
                        }

                        Divider()

                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "flip_card"
                                selectedPlan = plan.planName
                                showFlipCard = true
                                flipCardState = "menu"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_LISTEN_MENU -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划")
                        }

                        Divider()

                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "listening_test"
                                selectedPlan = plan.planName
                                showListeningTest = true
                                listeningTestState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_CHINESE_MENU_SELECT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划（中译英选择填词）")
                        }

                        Divider()

                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "chinese_select"
                                selectedPlan = plan.planName
                                showChineseToEnglishSelect = true
                                chineseSelectScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_CHINESE_MENU_SPELL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划")
                        }

                        Divider()

                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "chinese_plus"
                                selectedPlan = plan.planName
                                showChineseToEnglish = true
                                chineseTestScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_SENTENCE_MENU -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划")
                        }

                        Divider()

                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "sentence"
                                selectedPlan = plan.planName
                                showWordSentencePage = true
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_MANAGE_MENU -> {
                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.MAIN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("返回")
                        }

                        Divider()

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                selectedPlanToManage = plan.planName
                                drawerLevel = DrawerLevel.PLAN_MANAGE_ACTIONS
                            }
                        }
                    }

                    DrawerLevel.PLAN_MANAGE_ACTIONS -> {
                        val context = LocalContext.current

                        // 返回按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MANAGE_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("返回")
                        }

                        Divider()

                        DrawerText("🗑 删除该计划") {
                            selectedPlanToManage?.let {
                                FileHelper.deletePlan(context, it)
                            }
                            selectedPlanToManage = null
                            drawerLevel = DrawerLevel.MAIN_MENU
                        }
                    }

                    DrawerLevel.PLAN_MEANING_SELECT -> {
                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        // 返回按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划（以意选词）")
                        }

                        Divider()

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "meaning_select"
                                selectedPlan = plan.planName
                                showMeaningSelect = true
                                meaningSelectScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_SELF_MEANING_SELECT -> {
                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        // 返回按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.SELF_PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划（以意选词）")
                        }

                        Divider()

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "meaning_select_self"
                                selectedPlan = plan.planName
                                showMeaningSelect = true
                                meaningSelectScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_WORD_MEANING_SELECT -> {
                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划（以词选意）")
                        }

                        Divider()

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "word_meaning_select"
                                selectedPlan = plan.planName
                                showWordToMeaningSelect = true
                                wordToMeaningSelectScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }

                    DrawerLevel.PLAN_SELF_WORD_MEANING_SELECT -> {
                        val context = LocalContext.current
                        val plans = remember { FileHelper.loadAllPlans(context) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drawerLevel = DrawerLevel.SELF_PLAN_MENU }
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            Spacer(Modifier.width(8.dp))
                            DrawerText("选择计划（以词选意）")
                        }

                        Divider()

                        plans.forEach { plan ->
                            DrawerText(plan.planName) {
                                learningPlanTarget = "word_meaning_select_self"
                                selectedPlan = plan.planName
                                showWordToMeaningSelect = true
                                wordToMeaningSelectScreenState = "setup"
                                scope.launch { drawerState.close() }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (showLearningPlan) {
                LearningPlanScreen(
                    onBack = { showLearningPlan = false },
                    onPlanSelected = { planName ->
                        selectedPlan = planName
                        // TODO: 后续可以根据 planName 加载具体词表页面
                        showLearningPlan = false
                    }
                )
            } else {
                when {
                    showWordList -> {
                        WordListScreen(
                            context = LocalContext.current,
                            getDefinition = { viewModel.getDefinition(it) },
                            viewModel = viewModel,
                            onBackToMenu = { showWordList = false }
                        )
                    }

                    showChineseToEnglishSelect -> {
                        when (chineseSelectScreenState) {
                            "setup" -> ChineseToEnglishSelectSetup(
                                context = LocalContext.current,
                                viewModel = viewModel,
                                planSource = if (learningPlanTarget == "chinese_select")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                                selectedPlanName = selectedPlan,
                                onBack = { showChineseToEnglishSelect = false },
                                onStartTest = {
                                    testQuestions = it
                                    chineseSelectScreenState = "test"
                                }
                            )

                            "test" -> ChineseToEnglishSelect(
                                questions = testQuestions,
                                onFinish = {
                                    testResults = it
                                    chineseSelectScreenState = "result"
                                },
                                onBack = {
                                    chineseSelectScreenState = "setup"
                                }
                            )

                            "result" -> TestResultScreen(
                                results = testResults,
                                onBack = { showChineseToEnglishSelect = false },
                                onRetry = {
                                    testQuestions = testResults.shuffled().map { r -> r.word to r.chinese }
                                    chineseSelectScreenState = "test"
                                }
                            )
                        }
                    }

                    showChineseToEnglish -> {  //拼写版 Spell 的完整逻辑
                        when (chineseTestScreenState) {
                            "setup" -> ChineseToEnglishSetup(
                                context = LocalContext.current,
                                viewModel = viewModel,
                                planSource = if (learningPlanTarget == "chinese_plus")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS, // ✅ 动态传入词源
                                selectedPlanName = selectedPlan,
                                onBack = { showChineseToEnglish = false },
                                onStartTest = {
                                    testQuestions = it
                                    chineseTestScreenState = "test"
                                }
                            )

                            "test" -> ChineseToEnglishTest(
                                questions = testQuestions,
                                onFinish = {
                                    testResults = it
                                    chineseTestScreenState = "result"
                                },
                                onBack = {
                                    chineseTestScreenState = "setup"
                                }
                            )

                            "result" -> TestResultScreen(
                                results = testResults,
                                onBack = { showChineseToEnglish = false },
                                onRetry = {
                                    testQuestions = testResults.shuffled().map { r -> r.word to r.chinese }
                                    chineseTestScreenState = "test"
                                }
                            )
                        }
                    }  //// 拼写版 Spell 的完整逻辑

                    showListeningTest -> {
                        when (listeningTestState) {
                            "setup" -> ListeningTestSetupScreen(
                                context = LocalContext.current,
                                planSource = if (learningPlanTarget == "listening_test")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                                selectedPlanName = selectedPlan,
                                onStartTest = { words ->
                                    listeningWordList = words.shuffled()
                                    listeningTestState = "test"
                                },
                                onBack = {
                                    showListeningTest = false
                                }
                            )

                            "test" -> ListeningTestScreen(
                                context = LocalContext.current,
                                words = listeningWordList,
                                viewModel = viewModel,
                                onFinish = { results ->
                                    listeningResults = results
                                    listeningTestState = "result"
                                },
                                onBack = {
                                    listeningTestState = "setup"
                                }
                            )

                            "result" -> ListeningTestResultScreen(
                                results = listeningResults,
                                onRetry = {
                                    listeningWordList = listeningResults.map { it.first }.shuffled()
                                    listeningTestState = "test"
                                },
                                onExit = {
                                    showListeningTest = false
                                }
                            )
                        }
                    }

                    showFlipCard -> {
                        when (flipCardState) {
                            "menu" -> FlipCardMenuScreen(
                                context = LocalContext.current,
                                planSource = if (learningPlanTarget == "flip_card")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS, // ✅ 动态判断词源
                                selectedPlanName = selectedPlan,
                                onStartTest = { words ->
                                    flipCardWords = words
                                    flipCardState = "card"
                                },
                                onBack = {
                                    showFlipCard = false
                                    flipCardWords = emptyList()
                                    flipCardState = "menu"
                                }
                            )


                            "card" -> FlipCardScreen(
                                context = LocalContext.current,
                                wordList = flipCardWords,
                                viewModel = viewModel,
                                onExit = {
                                    showFlipCard = false
                                    flipCardWords = emptyList()
                                    flipCardState = "menu"
                                }
                            )
                        }
                    }

                    showWordSentencePage -> {
                        WordSentencePage(
                            context = LocalContext.current,
                            viewModel = viewModel,
                            planSource = if (learningPlanTarget == "sentence")
                                WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                            selectedPlanName = selectedPlan,
                            onExit = {
                                showWordSentencePage = false
                            }
                        )
                    }

                    showMeaningSelect -> {
                        when (meaningSelectScreenState) {
                            "setup" -> MeaningToWordSelectSetup(
                                context = LocalContext.current,
                                viewModel = viewModel,
                                planSource = if (learningPlanTarget == "meaning_select")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                                selectedPlanName = selectedPlan,
                                onBack = { showMeaningSelect = false },
                                onStartTest = {
                                    testQuestions = it
                                    meaningSelectScreenState = "test"
                                }
                            )

                            "test" -> MeaningToWordSelect(
                                questions = testQuestions,
                                viewModel = viewModel, // ✅ 传入
                                onFinish = {
                                    testResults = it
                                    meaningSelectScreenState = "result"
                                },
                                onBack = {
                                    meaningSelectScreenState = "setup"
                                }
                            )


                            "result" -> TestResultScreen(
                                results = testResults,
                                onBack = { showMeaningSelect = false },
                                onRetry = {
                                    testQuestions = testResults.shuffled().map { r -> r.word to r.chinese }
                                    meaningSelectScreenState = "test"
                                }
                            )
                        }
                    }

                    showWordToMeaningSelect -> {  // ✅ 插在这里
                        when (wordToMeaningSelectScreenState) {
                            "setup" -> WordToMeaningSelectSetup(
                                context = LocalContext.current,
                                viewModel = viewModel,
                                planSource = if (learningPlanTarget == "word_meaning_select")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                                selectedPlanName = selectedPlan,
                                onBack = { showWordToMeaningSelect = false },
                                onStartTest = {
                                    testQuestions = it
                                    wordToMeaningSelectScreenState = "test"
                                }
                            )

                            "test" -> WordToMeaningSelect(
                                questions = testQuestions,
                                viewModel = viewModel,
                                onFinish = {
                                    testResults = it
                                    wordToMeaningSelectScreenState = "result"
                                },
                                onBack = {
                                    wordToMeaningSelectScreenState = "setup"
                                }
                            )

                            "result" -> TestResultScreen(
                                results = testResults,
                                onBack = { showWordToMeaningSelect = false },
                                onRetry = {
                                    testQuestions = testResults.shuffled().map { r -> r.word to r.chinese }
                                    wordToMeaningSelectScreenState = "test"
                                }
                            )
                        }
                    }


                    else -> {
                        Column {
                            HomeScreen(
                                context = LocalContext.current,
                                viewModel = viewModel,
                                onStartClicked = { words -> // ✅ 明确接收 List<String> 参数
                                    flipCardWords = words     // ✅ 正确赋值
                                    showFlipCard = true
                                    flipCardState = "card"
                                },
                                onEnterLearningPlan = {
                                    showLearningPlan = true
                                }
                            )
                        }

                    }
                }
            }

            // 左下角菜单按钮
            IconButton(
                onClick = {
                    scope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "打开菜单")
            }
        }

    }
}


