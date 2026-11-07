// XhOper Desktop（Windows / Linux / macOS）
//
// 核心策略：**直接编译 Android 工程里的解析层源码**，不复制、不改写。
// 那些 android.* 调用由 src/main/kotlin/com/yunx/desktop/shim 下的 JVM 垫片提供，
// 因此各网盘的 API 逻辑只有一份真源，Android 端与桌面端永远同步。
//
// ★ 这里用「白名单」而不是「整目录 + exclude」：
//   Gradle 的 exclude 模式相对各个 srcDir 根解析，多 srcDir 并行时很容易漏掉
//   （此前 AudioExtractor / DashMuxer 就是这样混进来的）。白名单列出来的
//   文件才是参与编译的，多一个不多、少一个不少。
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

val androidSrcRoot = file("../app/src/main/kotlin/com/yunx/app")

/**
 * 复用的 Android 源文件（白名单）。
 *
 * 判断标准：纯 HTTP / 纯 JSON / 纯 JVM 逻辑，或只用到垫片能表达的 API
 * （Log、Base64、Context、SharedPreferences、Uri、Build）。
 */
val androidFiles = listOf(
    // ── 网盘 API：这是整个项目最有价值的部分，全部复用 ──
    "data/network/BaiduApi.kt",
    "data/network/BaiduApiException.kt",
    "data/network/BaiduConstants.kt",
    "data/network/C139Api.kt",
    "data/network/C139Constants.kt",
    "data/network/C139DeviceInfo.kt",
    "data/network/C139Kouling.kt",
    "data/network/DiagnosticNetworkInterceptor.kt",
    "data/network/GitHubApi.kt",
    "data/network/GitHubCommitDateCache.kt",
    "data/network/GitHubLinkParser.kt",
    "data/network/GitHubModels.kt",
    "data/network/GitHubResponseCache.kt",
    "data/network/GitHubTokenStore.kt",
    "data/network/GuangYaApi.kt",
    "data/network/GuangYaConstants.kt",
    "data/network/HttpClients.kt",
    "data/network/ILanzouApi.kt",
    "data/network/ILanzouConstants.kt",
    "data/network/LanzouApi.kt",
    "data/network/LanzouConstants.kt",
    "data/network/Pan115Api.kt",
    "data/network/Pan115Constants.kt",
    "data/network/Pan115Crypto.kt",
    "data/network/Pan123Api.kt",
    "data/network/Pan123Constants.kt",
    "data/network/Pan123DeviceId.kt",
    "data/network/Pan123LoginSupport.kt",
    "data/network/QuarkApi.kt",
    "data/network/QuarkApiException.kt",
    "data/network/QuarkCdn.kt",
    "data/network/QuarkConstants.kt",
    "data/network/QuarkUpload.kt",
    "data/network/ShareLinkParser.kt",
    "data/network/StarSeaApi.kt",
    "data/network/StarSeaConstants.kt",
    "data/network/UCApi.kt",
    "data/network/UCConstants.kt",
    "data/network/XunleiApi.kt",
    "data/network/XunleiConstants.kt",
    "data/network/XunleiDeviceFingerprint.kt",
    "data/network/XunleiKouling.kt",
    "data/network/XunleiWebCredential.kt",

    // ── 数据模型 ──
    "data/network/model/ShareExpire.kt",
    "data/network/model/ShareModels.kt",

    // ── 视频平台解析（除 HeadlessWebView：它依赖 WebView，桌面端不启用） ──
    "data/network/video/AcFunParser.kt",
    "data/network/video/BilibiliParser.kt",
    "data/network/video/DouyinParser.kt",
    "data/network/video/KuaishouParser.kt",
    "data/network/video/PixivParser.kt",
    "data/network/video/VideoHttp.kt",
    "data/network/video/VideoLinkParser.kt",
    "data/network/video/VideoModels.kt",
    "data/network/video/VideoParser.kt",
    "data/network/video/VideoParsers.kt",
    "data/network/video/VideoPlatform.kt",
    "data/network/video/WeiboParser.kt",
    "data/network/video/XhsParser.kt",

    // ── 下载相关的纯策略逻辑（不含 MediaCodec / 前台服务 / SAF 那几个） ──
    "data/download/DownloadPathPolicy.kt",
    "data/download/DownloadPlatform.kt",
    "data/download/DownloadSource.kt",
    "data/download/HlsRequestPolicy.kt",
    "data/download/HttpRangePolicy.kt",
    "data/download/MagnetLink.kt",

    // ── 其他纯 JVM 工具 ──
    "data/security/CredentialCipher.kt",
    "data/upload/UploadIo.kt",
    "data/upload/UploadSigning.kt",
    "data/upload/UploadSource.kt",
    "util/DiagnosticLog.kt",
    "util/LogRedactor.kt",
    "util/DownloadFileNames.kt"
)

sourceSets {
    main {
        // 桌面自有源码整目录纳入
        kotlin.srcDir("src/main/kotlin")

        // Android 白名单：**按目录分组**后每个目录只加一次 srcDir，
        // 再把该目录下不在白名单里的 .kt 文件排除掉。
        //
        // 为什么要分组：如果同一目录被 srcDir 多次（每个文件一次），
        // 那么最后一次的 exclude 会把前几次想要的文件也挡掉，结果一个都编不进来。
        androidFiles
            .map { File(androidSrcRoot, it) }
            .groupBy { it.parentFile }
            .forEach { (dir, files) ->
                kotlin.srcDir(dir)
                val wanted = files.map { it.name }.toSet()
                dir.listFiles()
                    ?.asSequence()
                    ?.filter { it.isFile && it.extension == "kt" && it.name !in wanted }
                    ?.forEach { kotlin.exclude(it.name) }
            }
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