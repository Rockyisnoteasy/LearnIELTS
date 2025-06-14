// 每一次导入新的.db字典文件，都要把version = 数字+1

package com.example.learnielts.data.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WordEntryEntity::class], version = 19, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            Log.d("调试", "💡 准备构建 Room 数据库")
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dictionary.db"
                )
                    .createFromAsset("dictionary.db") // 👈 从 assets 中读取已转好的 SQLite 文件
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
