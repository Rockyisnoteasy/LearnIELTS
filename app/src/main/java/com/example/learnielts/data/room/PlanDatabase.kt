package com.example.learnielts.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlanWordEntry::class], version = 1, exportSchema = false)
abstract class PlanDatabase : RoomDatabase() {
    abstract fun planWordDao(): PlanWordDao

    companion object {
        @Volatile private var INSTANCE: PlanDatabase? = null

        fun getInstance(context: Context, dbName: String): PlanDatabase {
            val assetPath = "四六级/$dbName"
            val dbPath = context.getDatabasePath(dbName).absolutePath

            try {
                android.util.Log.d("调试", "💡 准备加载词表数据库：$assetPath → $dbPath")
                return INSTANCE ?: synchronized(this) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        PlanDatabase::class.java,
                        dbName
                    )
                        .createFromAsset(assetPath)
                        .build()
                        .also { INSTANCE = it }
                }
            } catch (e: Exception) {
                android.util.Log.e("调试", "❌ 数据库加载失败：${e.message}")
                throw e
            }
        }

    }
}
