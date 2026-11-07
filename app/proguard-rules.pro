# XhOper R8 / ProGuard 规则
#
# 仅供 release / optimized 变体使用（debug 不混淆）。
# 本文件是「优化版能否正常运行」的关键：YunX 用到 Room、OkHttp、Coil、JNI 与少量反射，
# 任何一处被 R8 裁掉都会在运行时才炸，所以宁可多 keep 也不要少 keep。

# ---------- 通用：保留行号，崩溃栈才可读 ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 注解、泛型签名、内部类名：反射 / 序列化 / Room 都依赖
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ---------- Kotlin ----------
# Kotlin 元数据与协程状态机：被裁剪后 suspend 函数会抛 IllegalStateException
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ---------- Room ----------
# Room 生成的实现类由反射按名字查找（RoomDatabase 用 Class.forName）
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# ---------- 反射：反篡改探测用 Class.forName 判断类是否存在 ----------
# 这几个包名是 TextCipher 加密后的目标；保留「类名不被混淆」即可，
# 这里保守地整包 keep，避免 R8 把探测目标全部内联掉导致探测失效。
-keep class com.yunx.app.crash.** { *; }
-keep class com.yunx.app.util.ArchiveProbe { *; }
-keep class com.yunx.app.util.TextCipher { *; }
-keep class com.yunx.app.LaunchGate { *; }
-keep class com.yunx.app.YunXApp { *; }

# ---------- JNI（Gopeed 下载内核）----------
# System.loadLibrary("gojni") 与 native 方法签名必须原样保留
-keepclasseswithmembernames class * { native <methods>; }
-keep class com.yunx.app.data.gopeed.** { *; }

# ---------- OkHttp / Okio ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepclassmembers class * {
    @okhttp3.internal.annotations.EverythingIsNonNull <methods>;
}

# ---------- Coil / 图片 ----------
-dontwarn coil3.**

# ---------- 枚举：values() / valueOf() 被反射调用 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Parcelable ----------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---------- 序列化：JSON 手写解析，但保留 Bean 的 setter/getter ----------
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# ---------- 不要裁剪掉日志里的类名（诊断模式导出日志用） ----------
-keepnames class com.yunx.app.data.download.DownloadDebugLog

# ---------- 警告抑制：这些库的可选依赖未引入 ----------
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
