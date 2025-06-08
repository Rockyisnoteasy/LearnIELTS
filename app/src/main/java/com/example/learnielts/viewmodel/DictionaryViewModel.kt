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

    init {
        loadDict()
    }

    private fun loadDict() {
        viewModelScope.launch {
            try {
                Log.d("调试", "📦 ViewModel 开始调用 DictionaryLoader")
                val data = DictionaryLoader.loadDictionary(getApplication())
                _entries.value = data
                Log.d("调试", "✅ ViewModel 词典数据更新，项数=${data.size}")
            } catch (e: Exception) {
                Log.e("调试", "❌ ViewModel 中加载词典失败：${e.message}", e)
            }
        }
    }


    fun getDefinition(word: String): String? {
        val realWord = RelatedWordsManager.getRealSourceWord(word, appContext)
        return _entries.value.find { it.word.equals(realWord, ignoreCase = true) }?.definition
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


