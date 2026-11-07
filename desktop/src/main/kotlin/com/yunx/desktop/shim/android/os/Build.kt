/*
 * Shim: android.os.Build / Environment / Handler / Process
 *
 * Build.* 被设备指纹代码读（139、迅雷），桌面端给一个「Windows 10 桌面」的固定画像：
 * 这些字段只用于让服务端不把我们当异常客户端，不需要真实。
 */
package android.os

object Build {
    const val MANUFACTURER = "Microsoft"
    const val BRAND = "Windows"
    const val MODEL = "Desktop"
    const val DEVICE = "desktop"
    const val PRODUCT = "windows"
    const val FINGERPRINT = "microsoft/windows/desktop:10/RS5/1:user/release-keys"
    const val ID = "RS5"
    const val TAGS = "release-keys"
    const val TYPE = "user"
    const val HARDWARE = "x86_64"
    const val BOARD = "windows"
    const val DISPLAY = "1.0"
    const val BOOTLOADER = "unknown"
    const val HOST = "desktop"
    const val USER = "user"

    object VERSION {
        @JvmField
        val SDK_INT: Int = 34
        @JvmField
        val RELEASE: String = "14"
        @JvmField
        val CODENAME: String = "REL"
        @JvmField
        val INCREMENTAL: String = "1"
        @JvmField
        val SDK: String = "34"

        @JvmStatic
        fun getSdkIntCompat(): Int = SDK_INT
    }

    object VERSION_CODES {
        const val LOLLIPOP = 21
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
    }
}

object Environment {
    private const val ROOT = "YUNX_DESKTOP_ROOT"

    /** 桌面端「外部存储」= 用户下载目录 */
    private fun externalRoot(): java.io.File =
        java.io.File(System.getProperty("user.home"), "Downloads")

    @JvmStatic
    fun getExternalStorageDirectory(): java.io.File = externalRoot()

    @JvmStatic
    fun getExternalStoragePublicDirectory(type: String?): java.io.File =
        java.io.File(externalRoot(), type ?: "")

    @JvmStatic
    fun getDataDirectory(): java.io.File =
        java.io.File(System.getProperty("java.io.tmpdir"))

    @JvmStatic
    fun isExternalStorageManager(): Boolean = true

    const val DIRECTORY_DOWNLOADS = "Download"
    const val DIRECTORY_MUSIC = "Music"
    const val DIRECTORY_MOVIES = "Movies"
    const val DIRECTORY_PICTURES = "Pictures"
    const val DIRECTORY_DCIM = "DCIM"
    const val DIRECTORY_DOCUMENTS = "Documents"
}

object Process {
    @JvmStatic
    fun myPid(): Int = ProcessHandle.current().pid().toInt()

    @JvmStatic
    fun myTid(): Int = Thread.currentThread().threadId().toInt()
}

object SystemClock {
    @JvmStatic
    fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    @JvmStatic
    fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
    @JvmStatic
    fun currentThreadTimeMillis(): Long = System.nanoTime() / 1_000_000
    @JvmStatic
    fun sleep(ms: Long) = Thread.sleep(ms)
}