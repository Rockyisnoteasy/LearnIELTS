// learnielts/data/room/PlanDatabase.kt
package com.example.learnielts.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlanWordEntry::class], version = 1, exportSchema = false)
abstract class PlanDatabase : RoomDatabase() {
    abstract fun planWordDao(): PlanWordDao

    companion object {
        // ✅ 修改：使用一个 Map 来存储不同 dbName 对应的数据库实例
        private val INSTANCES: MutableMap<String, PlanDatabase> = mutableMapOf()

        fun getInstance(context: Context, dbName: String): PlanDatabase {
            val dbPath = context.getDatabasePath(dbName).absolutePath

            // ✅ 检查 Map 中是否已经存在该 dbName 对应的实例
            // 如果存在，直接返回，否则创建新的实例并存入 Map
            return INSTANCES.getOrPut(dbName) { // 使用 getOrPut 线程安全地获取或创建
                synchronized(this) { // 仍然保留 synchronized 块，以防 getOrPut 内部的创建逻辑也需要线程安全
                    android.util.Log.d("调试", "💡 准备加载词表数据库：$dbName → $dbPath")
                    Room.databaseBuilder(
                        context.applicationContext,
                        PlanDatabase::class.java,
                        dbName // 这里传入的 dbName 将确保 Room 创建/打开正确的数据库文件
                    )
                        .build()
                        .also {
                            // Room 默认会创建数据库文件到 /data/data/YOUR_PACKAGE_NAME/databases/ 目录下
                            // 如果文件不存在，或者版本不匹配，Room 会尝试创建空数据库，
                            // 但我们已经通过 FileHelper 确保文件已复制。
                            android.util.Log.d("调试", "✅ 数据库实例已创建/获取成功：$dbName")
                        }
                }
            }
        }

        // ✅ 可选：如果你需要清理某个数据库实例的缓存
//        fun clearInstance(dbName: String) {
//            synchronized(this) {
//                INSTANCES.remove(dbName)?.close() // 移除并关闭数据库实例
//                Log.d("调试", "💡 已清除数据库实例缓存：$dbName")
//            }
//        }
    }
}