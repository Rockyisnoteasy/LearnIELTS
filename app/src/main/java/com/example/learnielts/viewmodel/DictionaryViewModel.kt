// 控制 TTS 播放源、释义查询、播放行为等,MainActivity、HomeScreen、DictionarySearchBar调用

package com.example.learnielts.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnielts.data.DictionaryLoader
import com.example.learnielts.data.WordEntry
import com.example.learnielts.data.datasource.GoogleTTSDataSource
import com.example.learnielts.data.datasource.TencentTTSDataSource
import com.example.learnielts.data.datasource.TTSDataSource  // 新增接口
import com.example.learnielts.data.repository.DictRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.learnielts.util.RelatedWordsManager
import org.json.JSONArray
import org.json.JSONException

enum class TTSProvider { Tencent, Google }

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private var ttsProvider: TTSProvider = TTSProvider.Google
    private val appContext = getApplication<Application>()


    private fun createTtsSource(): TTSDataSource {
        return when (ttsProvider) {
            TTSProvider.Tencent -> TencentTTSDataSource(getApplication())
            TTSProvider.Google -> GoogleTTSDataSource(getApplication())
        }
    }

    private var ttsSource: TTSDataSource = createTtsSource()
    private var dictRepository = DictRepository(getApplication(), ttsSource)

    private val _entries = MutableStateFlow<List<WordEntry>>(emptyList())
    val entries: StateFlow<List<WordEntry>> = _entries

    // ✅ 新增：一个高效的查找表，键是单词本身（小写），值是对应的完整词条
    private var wordMap: Map<String, WordEntry> = mapOf()

    // ✅ 新增：一个高效的反向查找表，键是“变形词”，值是它的“词源”
    private var relatedWordMap: Map<String, String> = mapOf()

    init {
        loadDict()
    }

    private fun loadDict() {
        viewModelScope.launch {
            try {
                Log.d("调试", "📦 ViewModel 开始调用 DictionaryLoader")
                val data = DictionaryLoader.loadDictionary(getApplication())
                _entries.value = data // UI用的列表依然更新

                // 构建主词查找表
                wordMap = data.associateBy { it.word.lowercase() }

                // 构建变形词到词源的反向查找表
                val tempRelatedMap = mutableMapOf<String, String>()
                data.forEach { entry ->
                    val relatedString = entry.relatedWords
                    // 检查 relatedWords 是否是一个有效的JSON数组字符串
                    if (relatedString != null && relatedString.startsWith("[") && relatedString.endsWith("]")) {
                        try {
                            // 使用安卓内置的JSON工具解析
                            val jsonArray = JSONArray(relatedString)
                            for (i in 0 until jsonArray.length()) {
                                val variant = jsonArray.optString(i)
                                if (variant.isNotBlank()) {
                                    tempRelatedMap[variant.lowercase()] = entry.word.lowercase()
                                }
                            }
                        } catch (e: JSONException) {
                            // 如果解析失败，则按旧的空格方式处理，保证兼容性
                            relatedString.trim { it <= ' ' || it == '[' || it == ']' || it == '"' }
                                .split(Regex("[\\s,]+"))
                                .forEach { variant ->
                                    if (variant.isNotBlank()) {
                                        tempRelatedMap[variant.lowercase()] = entry.word.lowercase()
                                    }
                                }
                        }
                    }
                }
                relatedWordMap = tempRelatedMap

                Log.d("调试", "✅ ViewModel 词典数据更新，并已构建（含JSON解析的）高效查找表")

            } catch (e: Exception) {
                Log.e("调试", "❌ ViewModel 中加载词典失败：${e.message}", e)
            }
        }
    }


// 【请暂时用这个带有诊断功能的版本，替换掉你的 getDefinition 函数】

    /**
     * ✅【最终优化版】
     * 使用预先构建好的Map来实现高效、瞬时的单词释义查找。
     */
    fun getDefinition(word: String): String? {
        val searchWord = word.trim().lowercase()
        if (searchWord.isBlank()) return null

        // 1. 直接在主词查找表中寻找
        val exactMatch = wordMap[searchWord]
        if (exactMatch != null) {
            return exactMatch.definition
        }

        // 2. 如果没找到，直接在变形词查找表中寻找它的词源
        val rootWord = relatedWordMap[searchWord]
        if (rootWord != null) {
            // 找到了词源后，再从主词查找表中获取释义
            return wordMap[rootWord]?.definition
        }

        // 如果都找不到，返回 null
        return null
    }


    fun playWord(word: String, context: Context, speed: Float = 1.0f) {
        if (word.isBlank()) {
            Log.d("调试", "word 是空的，跳过")
            return
        }

        Log.d("调试", "playWord called: $word")
        Log.d("调试", "即将调用 dictRepository.playWordAudio")

        viewModelScope.launch {
            dictRepository.playWordAudio(word.trim(), speed)
        }
    }


    fun setTtsProvider(provider: TTSProvider) {
        ttsProvider = provider
        ttsSource = createTtsSource()
        dictRepository = DictRepository(getApplication(), ttsSource)
    }

    fun getCurrentTtsProvider(): TTSProvider {
        return ttsProvider
    }

    fun queryByChineseKeyword(keyword: String): List<String> {
        if (keyword.isBlank()) return emptyList()

        return entries.value.filter { entry ->
            val def = entry.definition
            val start = def.indexOf("中文释义：")
            val end = def.indexOf("词性：")
            if (start != -1 && end != -1 && end > start) {
                val chinesePart = def.substring(start + 5, end).trim()
                keyword in chinesePart
            } else false
        }.map { it.word }
    }

    suspend fun getRandomDistractorWords(correct: String): List<String> {
        return dictRepository.getRandomDistractorWords(correct)
    }

    suspend fun getRandomDistractorDefinitions(correctWord: String): List<String> {
        return dictRepository.getRandomDistractorDefinitions(correctWord)
    }

}


