/*
 * Shim: android.content.pm.ApplicationInfo / PackageManager / ServiceInfo
 *
 * 业务层几乎不直接用，只为让 util 与 data 里的引用能编译。
 */
package android.content.pm

class ApplicationInfo {
    var packageName: String = "com.xhoper.desktop"
    var versionName: String = "1.4.0"
    var versionCode: Int = 14
    var flags: Int = 0
    val dataDir: String = ""
    val nativeLibraryDir: String = ""
    val sourceDir: String = ""
    val uid: Int = 0
    var metaData: android.os.Bundle? = null
}

class PackageManager {
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo()
    fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo = ApplicationInfo()
    fun getLaunchIntentForPackage(packageName: String): android.content.Intent? = null
    fun resolveActivity(intent: android.content.Intent, flags: Int): ResolveInfo? = null
    fun queryIntentActivities(intent: android.content.Intent, flags: Int): List<ResolveInfo> = emptyList()
    fun hasSystemFeature(feature: String): Boolean = true

    companion object {
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
        const val GET_META_DATA = 128
    }
}

class PackageInfo {
    var packageName: String = "com.xhoper.desktop"
    var versionName: String = "1.4.0"
    var versionCode: Int = 14
    var longVersionCode: Long = 14L
}

class ResolveInfo

class ServiceInfo {
    var name: String = ""
    var packageName: String = ""
    val foregroundServiceType: Int = 0
}