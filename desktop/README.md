/*
 * XhOper 桌面版 · 构建说明
 *
 * 这个目录是**独立 Gradle 工程**（自带 settings.gradle.kts），
 * 不参与上层 Android 构建，因此不会影响 APK 打包。
 *
 * ── 为什么能复用 Android 源码 ─────────────────────────────
 *
 * build.gradle.kts 里把 ../app/src/main/kotlin/com/yunx/app 下的
 * data/network、data/security 直接加进了 Kotlin 源集。
 * 那些文件里写的是 `import android.util.Log` 之类，本来在 JVM 上编不过；
 * 但 src/main/kotlin/com/yunx/desktop/shim 下提供了同名同包的实现，
 * 编译器就认为它们是普通 Kotlin 类，于是原代码一行不改就能编译。
 *
 * 垫片覆盖：android.util.Log / Base64、android.content.Context /
 * SharedPreferences / ContentResolver、android.net.Uri、android.os.Build、
 * android.webkit.WebView 系列、androidx.core.content.edit、androidx.webkit。
 *
 * 这意味着：**Android 端更新网盘接口后，桌面端重新编译即可同步**，
 * 不存在两份实现互相漂移的问题。
 *
 * ── 不在复用范围内的部分 ─────────────────────────────────
 *
 *   data/db/**          Room 注解处理器，JVM 上跑不了
 *   data/download/**    依赖前台服务、MediaCodec、SAF
 *   data/gopeed/**      依赖 Android .so 与 DexFile
 *   ui/**               Compose for Android，桌面端用 Compose Desktop 重写
 *
 * 对应替代实现见 core/ 目录。
 *
 * ── 构建 ─────────────────────────────────────────────────
 *
 * Windows（出 exe）：
 *   gradlew.bat packageReleaseExe
 *   产物：build/compose/binaries/main/exe/XhOper-1.4.0.exe
 *
 * Linux / macOS（本地验证）：
 *   ./gradlew run
 *   ./gradlew compileKotlin          # 只编译，最快发现问题
 *
 * 注意：jpackage 不能跨平台。要 Windows 的 exe 必须在 Windows 上打包，
 * 所以正式出包走 .github/workflows/build-desktop.yml（GitHub 的 windows-latest）。
 */