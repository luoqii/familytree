package com.familytree.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.familytree.app.data.model.FamilyMember
import com.familytree.app.data.model.Relationship

/**
 * FamilyTree Room 数据库
 */
@Database(
    entities = [FamilyMember::class, Relationship::class],
    version = 1,
    exportSchema = false
)
abstract class FamilyTreeDatabase : RoomDatabase() {

    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyTreeDatabase? = null

        fun getDatabase(context: Context): FamilyTreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyTreeDatabase::class.java,
                    "family_tree_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
