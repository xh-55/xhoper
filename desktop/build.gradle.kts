// XhOper Desktop（Windows / Linux / macOS）
//
// 核心策略：**直接编译 Android 工程里的解析层源码**，不复制、不改写。
// 那些 android.* 调用由 src/main/kotlin/com/yunx/desktop/shim 下的 JVM 垫片提供，
// 因此各网盘的 API 逻辑只有一份真源，Android 端与桌面端永远同步。
//
// 复用范围（白名单，见 androidSrcDirs）：
//   data/network/**  各网盘 / 视频平台的接口实现——整个项目的价值都在这里
//   data/upload/UploadSigning.kt, UploadIo.kt   OSS/COS 签名与上传工具（纯 JVM）
//   data/security/** 凭证加解密
//   util/LogRedactor.kt, DownloadFileNames.kt  纯工具
//
// 不复用、由桌面端自己实现的部分：
//   data/db/**（Room）        -> core/JsonStore 用 JSON 落盘
//   data/download/**（Android 服务/MediaCodec） -> core/DesktopDownloader
//   data/gopeed/**（.so 引擎）-> 不启用
//   data/network/video/HeadlessWebView.kt（WebView） -> 不启用
//   ui/**（Compose for Android）-> ui/ 下用 Compose Desktop 重写
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

/** 复用的 Android 源码目录（整目录纳入编译） */
val androidSrcDirs = listOf(
    "data/network",
    "data/security"
)

/** 复用的单个 Android 源文件 */
val androidSrcFiles = listOf(
    "data/upload/UploadSigning.kt",
    "data/upload/UploadIo.kt",
    "util/LogRedactor.kt",
    "util/DownloadFileNames.kt"
)

sourceSets {
    main {
        kotlin.srcDir("src/main/kotlin")
        androidSrcDirs.forEach { kotlin.srcDir(File(androidSrcRoot, it)) }
        androidSrcFiles.forEach { kotlin.srcDir(File(androidSrcRoot, it).parentFile) }
        // 单文件复用：同目录其它文件不参与编译
        kotlin.include {
            // 桌面自有源码
            it.startsWith(file("src/main/kotlin").path) ||
                // 整目录复用
                androidSrcDirs.any { d -> it.startsWith(File(androidSrcRoot, d).path) } ||
                // 单文件复用
                androidSrcFiles.any { f -> it == File(androidSrcRoot, f).path }
        }
        // WebView 无头浏览器在桌面端不启用（依赖 android.webkit）
        kotlin.exclude("**/HeadlessWebView.kt")
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