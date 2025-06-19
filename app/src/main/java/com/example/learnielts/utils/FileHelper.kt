// 通用工具类，负责读取/写入每日词表、计算学习进度等,HomeScreen、LearningPlanScreen、其他工具调用
package com.example.learnielts.utils

import android.content.Context
import android.util.Log
import com.example.learnielts.data.model.PlanResponse
import org.json.JSONArray
import java.io.File
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson
import com.example.learnielts.data.room.PlanWordDao
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.learnielts.data.room.PlanDatabase


object FileHelper {

    // 根据从服务器获取的数据，强制覆盖本地所有学习计划
    fun overwriteLocalPlansFromServer(context: Context, plansFromServer: List<PlanResponse>) {
        Log.d("调试", "🗃️ 开始执行本地文件覆盖操作...")
        val plansDir = context.getExternalFilesDir(null) ?: return
        val currentPlanFile = File(plansDir, "current_plan.json")
        val scheduleDir = File(plansDir, "word_schedule")

        // 1. 清空本地旧数据
        try {
            if (currentPlanFile.exists()) currentPlanFile.delete()
            if (scheduleDir.exists()) scheduleDir.deleteRecursively()
            Log.d("调试", "🗑️ 已清空本地旧的学习计划文件。")
        } catch (e: Exception) {
            Log.e("调试", "❌ 清空本地文件时出错: ${e.message}")
        }

        if (plansFromServer.isEmpty()) {
            Log.d("调试", "服务器上没有学习计划，操作结束。")
            return
        }

        // 2. 重建 word_schedule 目录
        scheduleDir.mkdirs()

        // 3. 重建 current_plan.json
        val localPlanInfos = plansFromServer.map { serverPlan ->
            PlanInfo(
                serverId = serverPlan.id,
                planName = serverPlan.planName,
                category = serverPlan.category,
                selectedPlan = serverPlan.selectedPlan,
                dailyCount = serverPlan.dailyCount
            )
        }
        try {
            val gson = Gson()
            currentPlanFile.writeText(gson.toJson(localPlanInfos))
            Log.d("调试", "✅ 已根据服务器数据重建 current_plan.json。")
        } catch (e: Exception) {
            Log.e("调试", "❌ 写入 current_plan.json 时出错: ${e.message}")
        }


        // 4. 重建每日单词文件
        plansFromServer.forEach { serverPlan ->
            val planSpecificDir = File(scheduleDir, serverPlan.planName)
            planSpecificDir.mkdirs()

            serverPlan.dailyWords.forEach { dailyWord ->
                try {
                    val dailyFile = File(planSpecificDir, "${dailyWord.wordDate}.json")
                    val jsonArray = JSONArray(dailyWord.words)
                    dailyFile.writeText(jsonArray.toString(2))
                } catch (e: Exception) {
                    Log.e("调试", "❌ 写入每日单词文件 ${dailyWord.wordDate}.json 时出错: ${e.message}")
                }
            }
        }
        Log.d("调试", "✅ 已根据服务器数据重建所有每日单词文件。")
        Log.d("调试", "🎉 本地文件同步完成！")
    }


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
        plan: PlanInfo, // 直接传入 PlanInfo 对象
        masteredWords: List<String> // 传入全局已掌握单词列表
    ): List<String> {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val targetDir = File(context.getExternalFilesDir("word_schedule"), plan.planName)
        val targetFile = File(targetDir, "$dateStr.json")

        if (targetFile.exists()) {
            Log.d("调试", "⏩ [${plan.planName}] 今日词表已存在，跳过生成")
            return try {
                val json = JSONArray(targetFile.readText())
                List(json.length()) { json.getString(it) }
            } catch (e: Exception) { emptyList() }
        }

        try {
            // 1. 获取当前计划已经学过的所有单词 (这部分逻辑不变)
            val learnedInThisPlan = mutableSetOf<String>()
            targetDir.mkdirs()
            targetDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    val arr = JSONArray(file.readText())
                    for (i in 0 until arr.length()) {
                        learnedInThisPlan.add(arr.getString(i).lowercase())
                    }
                }
            }
            Log.d("调试", "📖 [${plan.planName}] 已学单词读取完成，总数=${learnedInThisPlan.size}")

            // 2. 准备数据库 (这部分逻辑不变)
            withContext(Dispatchers.IO) {
                copyOrUpdatePlanDb(context, plan.category, plan.selectedPlan)
            }
            val dbName = "${plan.selectedPlan}.db"
            val db = PlanDatabase.getInstance(context, dbName)
            val dao = db.planWordDao()

            // 3. ✅【核心修改】准备一个包含所有需要排除的单词的列表
            // 这个列表包括了本计划已学的 和 所有计划中已彻底掌握的
            val wordsToExclude = (learnedInThisPlan + masteredWords).distinct()
            Log.d("调试", "  🚫 [${plan.planName}] 将排除 ${wordsToExclude.size} 个单词 (本计划已学 + 全局已掌握).")

            // 4. ✅【核心修改】调用我们刚刚在 DAO 中新增的、更高效的函数
            val newWords = withContext(Dispatchers.IO) {
                dao.getNewWordsExcluding(wordsToExclude, plan.dailyCount)
            }
            Log.d("调试", "✅ [${plan.planName}] 成功从数据库随机获取未学单词 ${newWords.size} 个")

            // 5. 将获取到的新词写入文件 (这部分逻辑不变)
            if (newWords.isNotEmpty()) {
                val jsonArr = JSONArray(newWords)
                targetFile.writeText(jsonArr.toString(2))
                Log.d("调试", "✅ [${plan.planName}] 今日词表写入完成 → $dateStr.json")
            } else {
                Log.w("调试", "⚠️ [${plan.planName}] 没有剩余可学习的新单词")
            }
            return newWords

        } catch (e: Exception) {
            Log.e("调试", "❌ [${plan.planName}] 生成今日词表出错：${e.message}")
            return emptyList()
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
                // ✅ 使用 Gson 解析
                val gson = Gson()
                val planListType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, PlanInfo::class.java).type
                val existingPlans: List<PlanInfo> = gson.fromJson(file.readText(), planListType)
                plans.addAll(existingPlans)
            } catch (e: Exception) {
                Log.e("调试", "❌ 读取 current_plan.json 出错：${e.message}")
            }
        }

        // 移除同名计划（避免重复）
        plans.removeAll { it.planName == newPlan.planName }
        plans.add(newPlan)

        // 写回文件
        val gson = Gson()
        file.writeText(gson.toJson(plans))
        Log.d("调试", "✅ 新计划已添加到 current_plan.json：${newPlan.planName}")
    }

    fun loadAllPlans(context: Context): List<PlanInfo> {
        val file = File(context.getExternalFilesDir(null), "current_plan.json")
        if (!file.exists()) return emptyList()

        return try {
            val gson = Gson()
            val planListType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, PlanInfo::class.java).type
            gson.fromJson(file.readText(), planListType)
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
            val allPlans = loadAllPlans(context).toMutableList()
            val beforeSize = allPlans.size
            allPlans.removeAll { it.planName == planName }

            if (allPlans.size < beforeSize) {
                val gson = Gson()
                planFile.writeText(gson.toJson(allPlans))
                Log.d("调试", "✅ 删除计划成功：$planName")
            }
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