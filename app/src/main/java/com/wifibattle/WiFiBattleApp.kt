package com.wifibattle

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application 入口
 *
 * 负责：
 * 1. Hilt 依赖注入初始化
 * 2. 全局异常兜底
 * 3. 初始化网络、房间等核心服务
 */
@HiltAndroidApp
class WiFiBattleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 设置全局异常兜底，防止游戏过程中崩溃退出
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e(TAG, "Uncaught exception on $thread", throwable)
        }
    }

    companion object {
        private const val TAG = "WiFiBattleApp"
        lateinit var instance: WiFiBattleApp
            private set
    }
}
