/*
 * Shim: android.content.Context
 *
 * 桌面端把「Context」实现成一个指向应用数据目录的小对象：
 *   filesDir / cacheDir / getSharedPreferences / packageName 等都有真实语义，
 *   其余（startActivity、ContentResolver、Service）提供空壳，保证原代码能编译、能跑。
 *
 * 业务代码调用最多的是：contentResolver、packageName、cacheDir、filesDir、
 * getSharedPreferences、getSystemService、applicationContext。
 */
package android.content

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.net.Uri
import java.io.File

abstract class Context {
    open val applicationContext: Context get() = this
    abstract val packageName: String
    abstract val filesDir: File
    abstract val cacheDir: File
    abstract fun getSharedPreferences(name: String?, mode: Int): SharedPreferences
    open fun getExternalFilesDir(type: String?): File? = File(filesDir, "external${type?.let { "/$it" } ?: ""}")
    open val externalCacheDir: File? get() = File(cacheDir, "external")
    open val contentResolver: ContentResolver get() = ContentResolver()
    open val packageManager: PackageManager get() = PackageManager()
    open val applicationInfo: ApplicationInfo get() = ApplicationInfo()
    open val resources: Resources get() = Resources()
    open val assets: AssetManager get() = AssetManager()
    open val packageCodePath: String get() = ""
    open fun getSystemService(name: String): Any? = null
    open fun startActivity(intent: Intent) = Unit
    open fun startService(intent: Intent): Any? = null
    open fun stopService(intent: Intent): Boolean = true
    open fun startForegroundService(intent: Intent): Any? = null
    open fun checkSelfPermission(permission: String): Int = PackageManager.PERMISSION_GRANTED
    open fun getDatabasePath(name: String): File = File(filesDir.parentFile ?: filesDir, "databases/$name")

    companion object {
        const val MODE_PRIVATE = 0
        const val MODE_WORLD_READABLE = 1
        const val MODE_WORLD_WRITEABLE = 2
        const val MODE_MULTI_PROCESS = 4
    }
}

/** 桌面端最常用的系统服务名 */
object ContextConstants {
    const val POWER_SERVICE = "power"
    const val CONNECTIVITY_SERVICE = "connectivity"
    const val TELEPHONY_SERVICE = "phone"
    const val NOTIFICATION_SERVICE = "notification"
    const val WINDOW_SERVICE = "window"
}
