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
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlanDatabase::class.java,
                    dbName
                )
                    .createFromAsset("四六级/$dbName") // e.g. 六级十天冲刺1591词.db
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
