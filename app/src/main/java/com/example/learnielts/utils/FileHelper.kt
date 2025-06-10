// 通用工具类，负责读取/写入每日词表、计算学习进度等,HomeScreen、LearningPlanScreen、其他工具调用
package com.example.learnielts.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.File
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.learnielts.utils.PlanInfo
import com.example.learnielts.data.room.PlanWordDao
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object FileHelper {

    fun getWordsForDate(context: Context, dateStr: String): List<String> {
        return try {
            val file = File(context.getExternalFilesDir("learned_words"), "$dateStr.json")
            if (!file.exists()) return emptyList()
            val json = JSONArray(file.readText())
            List(json.length()) { json.getString(it) }
        } catch (e: Exception) {
            Log.e("调试", "读取失败: ${e.message}")
            emptyList()
        }
    }

    fun saveWordsForDate(context: Context, dateStr: String, words: List<String>) {
        try {
            val dir = context.getExternalFilesDir("learned_words")
            if (dir != null) {
                dir.mkdirs()
                val file = File(dir, "$dateStr.json")
                val json = JSONArray(words)
                file.writeText(json.toString(2))
            } else {
                Log.e("调试", "外部存储目录不可用（getExternalFilesDir 返回 null）")
            }
        } catch (e: Exception) {
            Log.e("调试", "保存失败: ${e.message}")
        }
    }

    fun calculateProgress(context: Context, planName: String, category: String, selectedPlan: String): Pair<Int, Int> {
        val learnedDir = File(context.getExternalFilesDir("word_schedule"), planName)
        val learnedWords = learnedDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.flatMap {
                val arr = JSONArray(it.readText())
                List(arr.length()) { idx -> arr.getString(idx).lowercase() }
            }?.toSet()?.size ?: 0

        return try {
            // ✅ 在打开数据库前，确保文件已复制。这里必须用runBlocking，因为此函数不是suspend
            runBlocking { // 添加 runBlocking 包裹，确保 copyOrUpdatePlanDb 在 IO 线程执行
                withContext(Dispatchers.IO) {
                    copyOrUpdatePlanDb(context, category, selectedPlan) // selectedPlan 是不带 .db 后缀的原始文件名
                }
            }


            val dbName = "$selectedPlan.db"
            val db = com.example.learnielts.data.room.PlanDatabase.getInstance(context, dbName)
            val dao = db.planWordDao()
            Log.d("调试", "📊 数据库打开成功：$dbName")
            val total = runBlocking { // 保持 runBlocking 因为 calculateProgress 是非 suspend 函数
                withContext(Dispatchers.IO) { // 确保 Room 操作在IO线程执行
                    dao.countAllWords()
                }
            }
            Log.d("调试", "📊 总词数 = $total，已学 = $learnedWords")
            Pair(learnedWords, total)
        } catch (e: Exception) {
            Log.e("调试", "❌ 查询总词数失败：${e.message}")
            Pair(learnedWords, 0)
        }
    }

    suspend fun generateTodayWordListFromPlan(
        context: Context,
        category: String,
        selectedPlan: String, // 不带 .db 后缀的原始文件名
        planName: String,
        dailyCount: Int
    ) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val targetDir = File(context.getExternalFilesDir("word_schedule"), planName)
        val targetFile = File(targetDir, "$dateStr.json")

        if (targetFile.exists()) {
            Log.d("调试", "⏩ [$planName] 今日词表已存在，跳过生成")
            return
        }

        try {
            // 已学单词
            val learned = mutableSetOf<String>()
            targetDir.mkdirs()
            targetDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    val arr = JSONArray(file.readText())
                    for (i in 0 until arr.length()) {
                        learned.add(arr.getString(i).lowercase())
                    }
                }
            }

            Log.d("调试", "📖 已学单词读取完成，总数=${learned.size}")

            // ✅ 在打开数据库前，确保文件已复制
            withContext(Dispatchers.IO) { // 确保文件复制也在IO线程
                copyOrUpdatePlanDb(context, category, selectedPlan)
            }


            // 打开数据库
            val dbName = "$selectedPlan.db"
            val db = com.example.learnielts.data.room.PlanDatabase.getInstance(context, dbName)
            val dao = db.planWordDao()
            Log.d("调试", "📘 打开词表数据库成功：$dbName")
            val newWords = withContext(Dispatchers.IO) { // 确保 Room 操作在IO线程执行
                dao.getUnlearnedWords(learned.toList(), dailyCount)
            }
            Log.d("调试", "✅ 成功获取未学单词 ${newWords.size} 个")

            if (newWords.isNotEmpty()) {
                val jsonArr = JSONArray(newWords)
                targetFile.writeText(jsonArr.toString(2))
                Log.d("调试", "✅ [$planName] 今日词表写入完成 → $dateStr.json")
            } else {
                Log.w("调试", "⚠️ [$planName] 没有剩余可学习的单词")
            }

        } catch (e: Exception) {
            Log.e("调试", "❌ [$planName] 生成今日词表出错：${e.message}")
        }
    }

    fun getAvailableDbPlans(context: Context, category: String): List<String> {
        return try {
            val assetList = context.assets.list(category) ?: return emptyList()
            assetList
                .filter { it.endsWith(".db") }
                .map { it.removeSuffix(".db") }
        } catch (e: Exception) {
            Log.e("调试", "❌ 获取可用词表失败：${e.message}")
            emptyList()
        }
    }

    fun addPlanToCurrentList(context: Context, newPlan: PlanInfo) {
        val file = File(context.getExternalFilesDir(null), "current_plan.json")
        val plans = mutableListOf<PlanInfo>()

        // 加载已有计划
        if (file.exists()) {
            try {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val plan = PlanInfo(
                        obj.getString("planName"),
                        obj.getString("category"),
                        obj.getString("selectedPlan"),
                        obj.getInt("dailyCount")
                    )
                    plans.add(plan)
                }
            } catch (e: Exception) {
                Log.e("调试", "❌ 读取 current_plan.json 出错：${e.message}")
            }
        }

        // 移除同名计划（避免重复）
        plans.removeAll { it.planName == newPlan.planName }
        plans.add(newPlan)

        // 写回文件
        val newArr = JSONArray()
        for (p in plans) {
            val obj = JSONObject().apply {
                put("planName", p.planName)
                put("category", p.category)
                put("selectedPlan", p.selectedPlan)
                put("dailyCount", p.dailyCount)
            }
            newArr.put(obj)
        }

        file.writeText(newArr.toString(2))
        Log.d("调试", "✅ 新计划已添加到 current_plan.json：${newPlan.planName}")
    }

    fun loadAllPlans(context: Context): List<PlanInfo> {
        val file = File(context.getExternalFilesDir(null), "current_plan.json")
        if (!file.exists()) return emptyList()

        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                PlanInfo(
                    obj.getString("planName"),
                    obj.getString("category"),
                    obj.getString("selectedPlan"),
                    obj.getInt("dailyCount")
                )
            }
        } catch (e: Exception) {
            Log.e("调试", "❌ 解析 current_plan.json 出错：${e.message}")
            emptyList()
        }
    }

    fun getWordsForDate(context: Context, dateStr: String, planName: String): List<String> {
        return try {
            val file =
                File(context.getExternalFilesDir("word_schedule/$planName"), "$dateStr.json")
            if (!file.exists()) return emptyList()
            val json = JSONArray(file.readText())
            List(json.length()) { json.getString(it) }
        } catch (e: Exception) {
            Log.e("调试", "读取失败: ${e.message}")
            emptyList()
        }
    }

    fun deletePlan(context: Context, planName: String) {
        // 删除 word_schedule/<planName> 目录
        val wordScheduleDir = File(context.getExternalFilesDir(null), "word_schedule")
        val planDir = File(wordScheduleDir, planName)
        Log.d("调试", "准备删除目录：" + planDir.absolutePath)
        if (planDir.exists()) planDir.deleteRecursively()

        // 修改 current_plan.json
        val planFile = File(context.getExternalFilesDir(null), "current_plan.json")
        if (!planFile.exists()) return

        try {
            val arr = JSONArray(planFile.readText())
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("planName") != planName) {
                    newArr.put(obj)
                }
            }
            planFile.writeText(newArr.toString(2))
            Log.d("调试", "✅ 删除计划成功：$planName")
        } catch (e: Exception) {
            Log.e("调试", "❌ 删除失败：${e.message}")
        }
    }

    fun copyOrUpdateWordbookJson(context: Context) {
        val targetFile = File(context.filesDir, "wordbook_android.json")
        val assetManager = context.assets

        try {
            Log.d("调试", "📝 开始检查 wordbook_android.json 是否需要更新")

            // 读取 assets 中的文件内容并计算 hash
            val assetBytes = assetManager.open("wordbook_android.json").readBytes()
            val assetHash = assetBytes.contentHashCode()
            Log.d("调试", "📦 资源文件 hash = $assetHash")

            val shouldUpdate = if (!targetFile.exists()) {
                Log.d("调试", "📁 本地文件不存在，准备复制")
                true
            } else {
                val localHash = targetFile.readBytes().contentHashCode()
                Log.d("调试", "📁 本地文件 hash = $localHash")
                assetHash != localHash
            }

            if (shouldUpdate) {
                targetFile.outputStream().use { it.write(assetBytes) }
                Log.d("调试", "✅ 已复制并更新 wordbook_android.json 到内部存储")
            } else {
                Log.d("调试", "⏩ 文件一致，无需更新 wordbook_android.json")
            }

        } catch (e: Exception) {
            Log.e("调试", "❌ 更新 wordbook_android.json 失败：${e.message}")
        }
    }

    /**
     * 检查并复制/更新学习计划数据库文件到应用内部存储。
     * 如果目标文件不存在，或者 assets 中的文件与目标文件不一致，则复制。
     *
     * @param context 应用上下文
     * @param category 学习计划分类，例如 "四六级", "考研", "雅思" 等
     * @param rawDbFileName 原始数据库文件名 (不带.db后缀，例如 "考研大纲词汇5511词")
     */
    fun copyOrUpdatePlanDb(context: Context, category: String, rawDbFileName: String) {
        val fullDbFileName = "$rawDbFileName.db"
        // Room 数据库的默认存储路径是 context.getDatabasePath()
        val targetFile = context.getDatabasePath(fullDbFileName)
        val assetPath = "$category/$fullDbFileName" // 构建 assets 中的完整路径
        val assetManager = context.assets

        try {
            Log.d("调试", "📝 开始检查学习计划数据库 $fullDbFileName ($category) 是否需要更新")

            // 读取 assets 中的文件内容并计算 hash
            val assetBytes = assetManager.open(assetPath).readBytes()
            val assetHash = assetBytes.contentHashCode()
            Log.d("调试", "📦 资源文件 $assetPath hash = $assetHash")

            val shouldUpdate = if (!targetFile.exists()) {
                Log.d("调试", "📁 本地学习计划数据库 $fullDbFileName 不存在，准备复制")
                true
            } else {
                val localHash = targetFile.readBytes().contentHashCode()
                Log.d("调试", "📁 本地学习计划数据库 $fullDbFileName hash = $localHash")
                assetHash != localHash
            }

            if (shouldUpdate) {
                // 确保父目录存在 (即 databases 目录)
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { it.write(assetBytes) }
                Log.d("调试", "✅ 已复制并更新学习计划数据库 $fullDbFileName 到内部存储")
            } else {
                Log.d("调试", "⏩ 学习计划数据库 $fullDbFileName 文件一致，无需更新")
            }

        } catch (e: Exception) {
            Log.e("调试", "❌ 更新学习计划数据库 $fullDbFileName 失败：${e.message}")
        }
    }

}
