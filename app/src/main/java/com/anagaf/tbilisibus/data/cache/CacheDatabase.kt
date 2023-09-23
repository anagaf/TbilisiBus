package com.anagaf.tbilisibus.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RouteInfoEntity::class, StopEntity::class, ShapePointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun routeInfoDao(): RouteInfoDao

    companion object {
        @Volatile
        private var INSTANCE: CacheDatabase? = null

        fun getDatabase(context: Context): CacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CacheDatabase::class.java,
                    "cache_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}