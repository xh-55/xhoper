// XhOper Desktop（Windows / Linux / macOS）
//
// 核心策略：**直接编译 Android 工程里的解析层源码**，不复制、不改写。
// 那些 android.* 调用由 src/main/kotlin/com/yunx/desktop/shim 下的 JVM 垫片提供，
// 因此各网盘的 API 逻辑只有一份真源，Android 端与桌面端永远同步。
//
// 复用的整目录：
//   data/network/**     各网盘 / 视频平台的接口实现——整个项目的价值都在这里
//   data/download/**    只取 DownloadPathPolicy 等纯逻辑（见下方 exclude 说明）
//   data/security/**    凭证加解密
//   data/upload/**      OSS/COS 签名与上传工具（纯 JVM 部分）
//   util/**             日志脱敏、文件名处理等纯工具
//
// 不复用（由桌面端自己实现，见 core/）：
//   data/db/**       Room 注解处理器，JVM 上跑不了
//   data/gopeed/**   依赖 Android .so 与 DexFile
//   ui/**            Compose for Android，桌面端用 Compose Desktop 重写
plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.compose") version "1.9.0"
}

group = "com.xhoper"
version = "1.4.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)
}

/** Android 工程源码根目录 */
val androidSrcRoot = file("../app/src/main/kotlin/com/yunx/app")

/**
 * 不参与桌面编译的 Android 源文件。
 *
 * 都是「依赖垫片无法表达的 Android 能力」的文件：WebView / SAF /
 * MediaCodec / 前台服务 / Binder / 通知。桌面端要么有替代实现（core/），
 * 要么这个功能在桌面端不提供。
 */
val androidExcludes = listOf(
    // ── WebView 无头浏览器：桌面端不启用（视频平台解析会走纯 HTTP 分支） ──
    "data/network/video/HeadlessWebView.kt",

    // ── 通知 / 前台服务 / MediaStore 保存 ──
    "data/download/DownloadService.kt",
    "data/download/DownloadSaver.kt",

    // ── MediaCodec 音视频合轨（桌面端需 ffmpeg，第一版不启用） ──
    "data/download/DashMuxer.kt",
    "data/download/AudioExtractor.kt",
    "data/download/MotionPhotoWriter.kt",

    // ── Gopeed 内置引擎：依赖 Android .so 与 DexFile ──
    "data/gopeed",

    // ── 依赖 Intent / PackageManager / MediaStore / Manifest 的工具 ──
    "util/AppLinks.kt",
    "util/ArchiveProbe.kt",
    "util/DeepLinks.kt",
    "util/LinkOpener.kt",
    "util/LogExporter.kt",
    "util/PermissionState.kt",
    "util/StorageDirs.kt",

    // ── 上传的 UI 包装与 SAF 文件源（桌面端无 SAF，上传功能不启用） ──
    "data/upload/UploadRunner.kt",
    "data/upload/UploadSource.kt"
)

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")

        // ★ 把 Android 的解析层直接纳入编译
        kotlin.srcDir(File(androidSrcRoot, "data/network"))
        kotlin.srcDir(File(androidSrcRoot, "data/security"))
        kotlin.srcDir(File(androidSrcRoot, "data/upload"))
        kotlin.srcDir(File(androidSrcRoot, "data/download"))
        kotlin.srcDir(File(androidSrcRoot, "util"))

        // exclude 用的路径是「相对各 srcDir 根」的模式，按文件名匹配最稳
        kotlin.exclude(androidExcludes)
    }
}

dependencies {
    // 与 Android 端版本保持一致
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "com.yunx.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = "XhOper"
            packageVersion = "1.4.0"
            description = "XhOper 云析 · 网盘 / 视频解析下载器（桌面版）"
            vendor = "XhOper"
            copyright = "AGPL-3.0"

            windows {
                menuGroup = "XhOper"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}