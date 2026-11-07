/*
 * Shim: android.content.res.Resources / AssetManager / Configuration / DisplayMetrics
 *
 * C139DeviceInfo 需要从 Resources 读屏幕分辨率与语言，用于拼 139 的
 * x-DeviceInfo 请求头。桌面端给一个固定的桌面画像（1920x1080 / 中文）。
 */
package android.content.res

import java.util.Locale

class DisplayMetrics {
    var widthPixels: Int = 1920
    var heightPixels: Int = 1080
    var densityDpi: Int = 160
    var density: Float = 1.0f
    var scaledDensity: Float = 1.0f
    var xdpi: Float = 160f
    var ydpi: Float = 160f
}

class Configuration {
    /** 与 Android 的 Configuration.locales 对齐 */
    val locales: Array<Locale> = arrayOf(Locale.getDefault())
    var locale: Locale = Locale.getDefault()
    var orientation: Int = ORIENTATION_PORTRAIT
    var screenWidthDp: Int = 1920
    var screenHeightDp: Int = 1080
    var smallestScreenWidthDp: Int = 1080
    var densityDpi: Int = 160
    var fontScale: Float = 1.0f

    companion object {
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}

class AssetManager {
    fun open(path: String): java.io.InputStream = throw java.io.FileNotFoundException(path)
    fun list(path: String): Array<String> = emptyArray()
}

class Resources {
    val displayMetrics: DisplayMetrics = DisplayMetrics()
    val configuration: Configuration = Configuration()
    fun getDisplayMetrics(): DisplayMetrics = displayMetrics
    fun getConfiguration(): Configuration = configuration
    fun getString(id: Int): String = ""
    fun getString(id: Int, vararg args: Any?): String = ""
    fun getDimension(id: Int): Float = 0f
    fun getDimensionPixelSize(id: Int): Int = 0
}