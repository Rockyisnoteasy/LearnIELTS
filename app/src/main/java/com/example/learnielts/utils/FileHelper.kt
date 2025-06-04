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

    fun calculateProgress(context: Context, planName: String): Pair<Int, Int> {
        val learnedDir = File(context.getExternalFilesDir("word_schedule"), planName)
        val learnedWords = learnedDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.flatMap {
                val arr = JSONArray(it.readText())
                List(arr.length()) { idx -> arr.getString(idx).lowercase() }
            }?.toSet()?.size ?: 0

        val total = 4781 // 后续可考虑按词表总数动态读取
        return Pair(learnedWords, total)
    }


//    fun saveCurrentPlan(
//        context: Context,
//        planName: String,
//        category: String,
//        selectedPlan: String,
//        dailyCount: Int
//    ) {
//        val json = JSONObject().apply {
//            put("planName", planName)
//            put("category", category)
//            put("selectedPlan", selectedPlan)
//            put("dailyCount", dailyCount)
//        }
//
//        val file = File(context.getExternalFilesDir(null), "current_plan.json")
//        file.writeText(json.toString(2))
//        Log.d("调试", "✅ current_plan.json 已保存：$planName")
//    }

    fun generateTodayWordListFromPlan(
        context: Context,
        category: String,
        selectedPlan: String,
        planName: String,
        dailyCount: Int
    ) {
        val assetPath = "$category/$selectedPlan.xlsx"
        val allWords = ExcelWordLoader.loadWordsFromAsset(context, assetPath)

        // 读取已学过的单词（仅限该计划名的子目录）
        val learnedWordsDir = File(context.getExternalFilesDir("word_schedule"), planName)
        val learned = mutableSetOf<String>()
        learnedWordsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".json")) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    learned.add(arr.getString(i).lowercase())
                }
            }
        }

        val unseenWords = allWords.filterNot { it.lowercase() in learned }.shuffled()
        val todayWords = unseenWords.take(dailyCount)

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val targetDir = File(context.getExternalFilesDir("word_schedule"), planName)
        val targetFile = File(targetDir, "$dateStr.json")

        targetDir.mkdirs()
        if (!targetFile.exists()) {
            targetFile.writeText(JSONArray(todayWords).toString(2))
            Log.d(
                "调试",
                "✅ [$planName] 今日学习单词已生成，共 ${todayWords.size} 个 → $dateStr.json"
            )
        } else {
            Log.d("调试", "ℹ️ [$planName] 今日学习单词已存在，跳过生成 → $dateStr.json")
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


}
