package com.familytree.app

import android.app.Application

/**
 * FamilyTree 应用程序主入口类
 * 用于初始化全局状态和依赖
 */
class FamilyTreeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化数据库、依赖注入等
    }
}
