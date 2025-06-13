// learned word里面的文件，是当日单词列表，可以与pyhon互通
// csv字典文件放在assets文件夹，wordbook_android.json，也就是映射文件，放在files文件夹下面
// 语音缓存文件也放在files文件夹下面。
// 应用主入口，管理页面导航、状态变量（如是否展示首页、翻牌页等）
// 当选择单词播放语音时，会先检查/storage/emulated/0/Android/data/com.example.learnielts/files/voice_cache
// 下面是否有缓存，如果没有，就调用TTS API，并把语音缓存保留在/storage/emulated/0/Android/data/com.example.learnielts/files，
// 同时，把单词-语音映射关系写在/data/data/com.example.learnielts/files/wordbook_android.json
// // 每一次导入新的.db字典文件，都要把com.example.learnielts.data.room.database 文件里面的version = 数字+1
// 在AuthViewModel.kt，修改每次app向服务器问询自身登录状态的时间，目前是5分钟。
// 用户创建学习计划后，每日的学习计划word_schedule 目录会上传到数据库的plan_daily_words，而current_plan.json会上传到数据库的learning_plans
// csv词典，要先清洗*,```，plaintext,markdown,和单词：前面的\n，才可以转换为.db文件，词典.db文件可以提取出word，然后从提取出的word里面，跟word_book.json
// 里面的单词做对比，不在里面的，就说明没有语音，就调取谷歌TTS。


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
import com.example.learnielts.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.ui.screen.LoginScreen
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.learnielts.utils.ChineseDefinitionExtractor


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
    PLAN_SELF_WORD_MEANING_SELECT
}

class MainActivity : ComponentActivity() {
    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FileHelper.copyOrUpdateWordbookJson(this)

        setContent {
            LearnIELTSTheme {
                AppRoot()  // ✅ 这是你刚刚写的封装了登录判断的根函数
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
fun AppRoot() {
    val authViewModel: AuthViewModel = viewModel()
    val dictionaryViewModel: DictionaryViewModel = viewModel()

    val profile by authViewModel.profile.collectAsState()
    val loggedOut by authViewModel.loggedOut.collectAsState()
    val showLogin = remember { mutableStateOf(profile == null) }

    // 监听 loggedOut 状态
    LaunchedEffect(loggedOut) {
        if (loggedOut) {
            showLogin.value = true
        }
    }

    if (showLogin.value) {
        LoginScreen(viewModel = authViewModel) {
            showLogin.value = false
            authViewModel.resetLogoutFlag()
        }
    } else {
        AppContent(
            viewModel = dictionaryViewModel,
            authViewModel = authViewModel,
            showLogin = showLogin // 这个引用是可变的
        )
    }

}

@Composable
fun AppContent(
    viewModel: DictionaryViewModel,
    authViewModel: AuthViewModel,
    showLogin: MutableState<Boolean>
) {

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
    var flipCardState by remember { mutableStateOf("menu") } // or "card"
    // 注意：我们将之前 flipCardWords 的功能扩展，用一个新的 state 来存储当前整个学习+测试环节的单词
    var wordsForCurrentSession by remember { mutableStateOf<List<String>>(emptyList()) }
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
    // ✅ --- 新增的状态变量 ---
    // 控制“是否开始测试”的第一个对话框
    var showStartTestDialog by remember { mutableStateOf(false) }
    // 控制“是否开始下一轮测试”的对话框
    var showNextTestDialog by remember { mutableStateOf(false) }
    // 标记当前是否处于“测试序列”中
    var isInTestSequence by remember { mutableStateOf(false) }
    // 标记“本轮不再提醒”是否被勾选
    var doNotRemindForSession by remember { mutableStateOf(false) }
    // 当前在测试序列中的位置索引
    var currentTestIndex by remember { mutableStateOf(0) }
    // 定义测试序列的顺序
    val testSequence = remember {
        listOf(
            "word_to_meaning_select", // 以词选意
            "meaning_to_word_select", // 以意选词
            "chinese_select",         // 选择填词
            "chinese_spell",          // 拼写填词
            "listening_test",         // 听力填空
            "word_sentence"           // 以词造句
        )
    }
    // 用于显示 Snackbar 提示
    val snackbarHostState = remember { SnackbarHostState() }
    // ✅ --- 核心修正点 1：将所有辅助函数和逻辑都定义在 AppContent 内部 ---

    // 辅助函数：启动一个具体的测试
    fun startCurrentTest() {
        if (currentTestIndex >= testSequence.size) return

        val testType = testSequence[currentTestIndex]
        val questions = when (testType) {
            "word_to_meaning_select", "meaning_to_word_select", "chinese_spell", "chinese_select" -> {
                wordsForCurrentSession.mapNotNull { word ->
                    viewModel.getDefinition(word)?.let { def ->
                        ChineseDefinitionExtractor.extract(def)?.let { chinese -> word to chinese }
                    }
                }.shuffled()
            }
            else -> emptyList()
        }

        testQuestions = questions
        when (testType) {
            "word_to_meaning_select" -> {
                wordToMeaningSelectScreenState = "test"; showWordToMeaningSelect = true
            }
            "meaning_to_word_select" -> {
                meaningSelectScreenState = "test"; showMeaningSelect = true
            }
            "chinese_select" -> {
                chineseSelectScreenState = "test"; showChineseToEnglishSelect = true
            }
            "chinese_spell" -> {
                chineseTestScreenState = "test"; showChineseToEnglish = true
            }
            "listening_test" -> {
                listeningWordList = wordsForCurrentSession.shuffled(); listeningTestState = "test"; showListeningTest = true
            }
            "word_sentence" -> {
                showWordSentencePage = true
            }
        }
    }

    // 可复用的 lambda：处理当一个测试完成时的逻辑
    val onTestFinished: (List<Any>) -> Unit = {
        currentTestIndex++
        if (currentTestIndex >= testSequence.size) {
            isInTestSequence = false
            scope.launch { snackbarHostState.showSnackbar("恭喜，所有测试已完成！") }
        } else {
            if (doNotRemindForSession) {
                startCurrentTest()
            } else {
                showNextTestDialog = true
            }
        }
    }

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

                        DrawerText(
                            text = "退出登录",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authViewModel.logout()
                                    showLogin.value = true
                                    scope.launch { drawerState.close() }
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
                        // ✅ 修正：在使用 planToDelete 之前，必须先定义它
                        val allPlans = remember { FileHelper.loadAllPlans(context) }
                        val planToDelete = allPlans.find { it.planName == selectedPlanToManage }

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
                            // 现在 planToDelete 已经被正确定义
                            planToDelete?.let {
                                authViewModel.deletePlanOnServer(it)
                            }
                            selectedPlanToManage = null
                            drawerLevel = DrawerLevel.MAIN_MENU
                            scope.launch { drawerState.close() }
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
                    // 选择填词
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
                                onFinish = { results ->
                                    showChineseToEnglishSelect = false // 1. 关闭当前屏幕
                                    if (isInTestSequence) {
                                        onTestFinished(results) // 2. 调用通用函数处理序列流转
                                    } else {
                                        // 3. 否则，走旧的独立测试逻辑
                                        testResults = results
                                        chineseSelectScreenState = "result"
                                    }
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
                    // 拼写填词
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
                                onFinish = { results ->
                                    showChineseToEnglish = false // 1. 关闭当前屏幕
                                    if (isInTestSequence) {
                                        onTestFinished(results) // 2. 调用通用函数处理序列流转
                                    } else {
                                        // 3. 否则，走旧的独立测试逻辑
                                        testResults = results
                                        chineseTestScreenState = "result"
                                    }
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

                    // 听力填空
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
                                    showListeningTest = false // 1. 关闭当前屏幕
                                    if (isInTestSequence) {
                                        onTestFinished(results) // 2. 调用通用函数处理序列流转
                                    } else {
                                        // 3. 否则，走旧的独立测试逻辑
                                        listeningResults = results
                                        listeningTestState = "result"
                                    }
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

                    // 翻牌记忆卡
                    showFlipCard -> {
                        when (flipCardState) {
                            "menu" -> FlipCardMenuScreen(
                                context = LocalContext.current,
                                planSource = if (learningPlanTarget == "flip_card")
                                    WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                                selectedPlanName = selectedPlan,
                                onStartTest = { words ->
                                    // 将当前要学习和测试的单词列表存入新的状态变量
                                    wordsForCurrentSession = words
                                    flipCardState = "card"
                                },
                                onBack = {
                                    showFlipCard = false
                                }
                            )

                            "card" -> FlipCardScreen(
                                context = LocalContext.current,
                                wordList = wordsForCurrentSession, // 使用我们为当前环节存储的单词列表
                                viewModel = viewModel,
                                // ✅ 当学习完成时，执行这里的逻辑
                                onSessionComplete = {
                                    showFlipCard = false // 1. 关闭记忆卡界面
                                    flipCardState = "menu" // 2. 重置状态，以便下次能从菜单进入
                                    showStartTestDialog = true // 3. 弹出“是否开始测试”的对话框
                                },
                                // ✅ 当用户中途按返回键时，执行这里的逻辑
                                onBackPress = {
                                    showFlipCard = false // 1. 直接关闭记忆卡界面
                                    flipCardState = "menu" // 2. 重置状态
                                }
                            )
                        }
                    }

                    // 以词造句
                    showWordSentencePage -> {
                        WordSentencePage(
                            context = LocalContext.current,
                            viewModel = viewModel,
                            planSource = if (learningPlanTarget == "sentence") WordPlanSource.SCHEDULED_PLAN else WordPlanSource.LEARNED_WORDS,
                            selectedPlanName = selectedPlan,
                            // ✅ 修正：传入当前测试环节的单词列表
                            initialWords = if (isInTestSequence) wordsForCurrentSession else null,
                            onExit = {
                                showWordSentencePage = false
                                if (isInTestSequence) {
                                    onTestFinished(emptyList())
                                }
                            }
                        )
                    }

                    // 以意选词
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
                                onFinish = { results ->
                                    showMeaningSelect = false // 1. 关闭当前屏幕
                                    if (isInTestSequence) {
                                        onTestFinished(results) // 2. 调用通用函数处理序列流转
                                    } else {
                                        // 3. 否则，走旧的独立测试逻辑
                                        testResults = results
                                        meaningSelectScreenState = "result"
                                    }
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

                    // 以词选意
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
                                onFinish = { results ->
                                    showWordToMeaningSelect = false // 1. 关闭当前屏幕
                                    if (isInTestSequence) {
                                        onTestFinished(results) // 2. 调用通用函数处理序列流转
                                    } else {
                                        // 3. 否则，走旧的独立测试逻辑
                                        testResults = results
                                        wordToMeaningSelectScreenState = "result"
                                    }
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
                                onStartClicked = { words ->
                                    wordsForCurrentSession = words // ✅ 修正：更新正确的变量
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

            // ✅ --- 新增的对话框和 Snackbar ---

            // 1. “是否开始测试？” 对话框
// 1. “是否开始测试？” 对话框
            if (showStartTestDialog) {
                AlertDialog(
                    onDismissRequest = { showStartTestDialog = false },
                    title = { Text("学习完成！") },
                    text = { Text("是否立即开始测试？") },
                    confirmButton = {
                        TextButton(onClick = {
                            showStartTestDialog = false
                            isInTestSequence = true
                            currentTestIndex = 0
                            // ✅ 调用辅助函数，并处理所有测试类型的回调
                            startCurrentTest(testSequence, 0, wordsForCurrentSession, viewModel) { screenName, questionsData ->
                                testQuestions = questionsData
                                when (screenName) {
                                    "word_to_meaning_select" -> {
                                        wordToMeaningSelectScreenState = "test"
                                        showWordToMeaningSelect = true
                                    }
                                    "meaning_to_word_select" -> {
                                        meaningSelectScreenState = "test"
                                        showMeaningSelect = true
                                    }
                                    "chinese_select" -> {
                                        chineseSelectScreenState = "test"
                                        showChineseToEnglishSelect = true
                                    }
                                    "chinese_spell" -> {
                                        chineseTestScreenState = "test"
                                        showChineseToEnglish = true
                                    }
                                    "listening_test" -> {
                                        listeningWordList = wordsForCurrentSession.shuffled()
                                        listeningTestState = "test"
                                        showListeningTest = true
                                    }
                                    "word_sentence" -> {
                                        // 以词造句没有独立的测试界面，可以直接启动
                                        // 这里可以先跳过或显示一个提示
                                    }
                                }
                            }
                        }) { Text("开始测试") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showStartTestDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "可随时在抽屉菜单开启测试",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) { Text("不了，谢谢") }
                    }
                )
            }

// 2. “是否继续下一轮测试？” 对话框
            if (showNextTestDialog) {
                var tempNoRemind by remember { mutableStateOf(doNotRemindForSession) }
                AlertDialog(
                    onDismissRequest = {
                        showNextTestDialog = false
                        isInTestSequence = false
                    },
                    title = { Text("本轮测试完成") },
                    text = {
                        Column {
                            Text("是否开始下一轮测试？")
                            Row(
                                Modifier.clickable { tempNoRemind = !tempNoRemind },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = tempNoRemind, onCheckedChange = { tempNoRemind = it })
                                Text("本轮不再提醒")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showNextTestDialog = false
                            doNotRemindForSession = tempNoRemind
                            // ✅ 调用辅助函数，并处理所有测试类型的回调
                            startCurrentTest(testSequence, currentTestIndex, wordsForCurrentSession, viewModel) { screenName, questionsData ->
                                testQuestions = questionsData
                                when (screenName) {
                                    "word_to_meaning_select" -> {
                                        wordToMeaningSelectScreenState = "test"
                                        showWordToMeaningSelect = true
                                    }
                                    "meaning_to_word_select" -> {
                                        meaningSelectScreenState = "test"
                                        showMeaningSelect = true
                                    }
                                    "chinese_select" -> {
                                        chineseSelectScreenState = "test"
                                        showChineseToEnglishSelect = true
                                    }
                                    "chinese_spell" -> {
                                        chineseTestScreenState = "test"
                                        showChineseToEnglish = true
                                    }
                                    "listening_test" -> {
                                        listeningWordList = wordsForCurrentSession.shuffled()
                                        listeningTestState = "test"
                                        showListeningTest = true
                                    }
                                    "word_sentence" -> {
                                        // 以词造句没有独立的测试界面，可以直接启动
                                    }
                                }
                            }
                        }) { Text("继续") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showNextTestDialog = false
                            isInTestSequence = false
                        }) { Text("结束测试") }
                    }
                )
            }

            // 3. 用于显示提示的 Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

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

// ✅ 在 MainActivity.kt 的末尾，或 AppContent 外部，添加这个新的辅助函数
private fun startCurrentTest(
    testSequence: List<String>,
    currentTestIndex: Int,
    words: List<String>,
    viewModel: DictionaryViewModel,
    // 使用一个回调函数来通知 MainActivity 更新具体某个屏幕的状态
    showScreen: (screenName: String, questions: List<Pair<String, String>>) -> Unit
) {
    if (currentTestIndex >= testSequence.size) return // 所有测试已完成

    val testType = testSequence[currentTestIndex]
    val questions = when (testType) {
        // ✅ 修正：为“以词选意”单独处理，传递完整释义
        "word_to_meaning_select" -> {
            words.mapNotNull { word ->
                viewModel.getDefinition(word)?.let { def ->
                    word to def // 直接传递 word 和完整的 definition
                }
            }.shuffled()
        }
        // ✅ 其他测试保持原有逻辑，传递提取后的中文释义
        "meaning_to_word_select", "chinese_spell", "chinese_select" -> {
            words.mapNotNull { word ->
                viewModel.getDefinition(word)?.let { def ->
                    ChineseDefinitionExtractor.extract(def)?.let { chinese ->
                        word to chinese
                    }
                }
            }.shuffled()
        }
        else -> emptyList()
    }

    // 调用回调，让 MainActivity 显示对应的屏幕
    showScreen(testType, questions)
}


