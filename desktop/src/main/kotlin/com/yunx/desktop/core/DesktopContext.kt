/*
 * 桌面端的 Context 实现。
 *
 * 所有数据落在平台约定目录：
 *   Windows : %APPDATA%\XhOper
 *   Linux   : ~/.config/xhoper
 *   macOS   : ~/Library/Application Support/XhOper
 *
 * 下载默认目录是用户「下载」文件夹（Windows 的 Downloads、Linux 的 ~/Downloads）。
 */
package com.yunx.desktop.core

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object DesktopContext : Context() {

    override val packageName: String = "com.xhoper.desktop"
    override val applicationContext: Context get() = this

    /** 应用数据根目录 */
    val dataRoot: File by lazy {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val home = File(System.getProperty("user.home") ?: ".")
        val dir = when {
            os.contains("win") -> File(System.getenv("APPDATA") ?: home.path, "XhOper")
            os.contains("mac") -> File(home, "Library/Application Support/XhOper")
            else -> File(System.getenv("XDG_CONFIG_HOME") ?: File(home, ".config").path, "xhoper")
        }
        dir.apply { mkdirs() }
    }

    /** 日志 / 临时文件 */
    override val cacheDir: File by lazy {
        File(dataRoot, "cache").apply { mkdirs() }
    }

    /** 配置与数据库 */
    override val filesDir: File by lazy {
        File(dataRoot, "files").apply { mkdirs() }
    }

    /** 默认下载目录 */
    val downloadDir: File by lazy {
        val home = File(System.getProperty("user.home") ?: ".")
        File(home, "Downloads").let { d ->
            if (d.isDirectory) File(d, "XhOper").apply { mkdirs() }
            else File(dataRoot, "downloads").apply { mkdirs() }
        }
    }

    private val prefsCache = HashMap<String, SharedPreferences>()
    private val prefsLock = Any()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = synchronized(prefsLock) {
        val key = name ?: "default"
        prefsCache.getOrPut(key) {
            File(filesDir, "$key.properties").let { PropertiesSharedPreferences(it) }
        }
    }

    override fun getSystemService(name: String): Any? = null

    /** 桌面端无权限概念，恒为已授权 */
    override fun checkSelfPermission(permission: String): Int = 0
}