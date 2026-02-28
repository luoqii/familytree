package com.familytree.app

import android.app.Application
import com.familytree.app.data.FamilyTreeDatabase

class FamilyTreeApplication : Application() {

    lateinit var database: FamilyTreeDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FamilyTreeDatabase.getDatabase(this)
    }
}
