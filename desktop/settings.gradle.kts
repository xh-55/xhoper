// XhOper Desktop —— 独立 Gradle 构建，与上层 Android 工程完全隔离。
//
// 为什么不加进 yunxapp/settings.gradle.kts 的 include：
//   那个工程装了 AGP，桌面模块一旦出现在同一构建里就会被打包流程污染，
//   而 Windows 版只需要 JDK + Kotlin，与 Android SDK 无关。
//
// 用法（本目录即根工程）：
//   Windows : gradlew.bat run   |  gradlew.bat packageReleaseExe
//   Linux   : ./gradlew run     |  ./gradlew packageReleaseExe
rootProject.name = "xhoper-desktop"

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
    }
}