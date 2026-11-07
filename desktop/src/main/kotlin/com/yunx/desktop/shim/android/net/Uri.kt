/*
 * Shim: android.net.Uri
 *
 * 桌面端没有 content:// 体系，但业务代码里 Uri 用法很轻：
 *  - Uri.parse(s) / Uri.fromFile(f) 构造；
 *  - .toString() / .path / .host / .lastPathSegment 读取；
 *  - DownloadSaver 里的 SAF 分支在桌面端不会走到。
 *
 * 所以这里实现成一个「字符串 + 预解析」的不可变值对象。
 */
package android.net

import java.io.File
import java.net.URI

class Uri private constructor(private val raw: String) {

    val scheme: String? get() = runCatching { URI(raw).scheme }.getOrNull()
    val host: String? get() = runCatching { URI(raw).host }.getOrNull()
    val port: Int get() = runCatching { URI(raw).port }.getOrDefault(-1)
    val path: String? get() = runCatching { URI(raw).path }.getOrNull()
    val query: String? get() = runCatching { URI(raw).rawQuery }.getOrNull()
    val fragment: String? get() = runCatching { URI(raw).fragment }.getOrNull()
    val authority: String? get() = runCatching { URI(raw).authority }.getOrNull()
    val userInfo: String? get() = runCatching { URI(raw).userInfo }.getOrNull()
    val isAbsolute: Boolean get() = scheme != null

    val lastPathSegment: String?
        get() = path?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }

    val pathSegments: List<String>
        get() = path?.split('/')?.filter { it.isNotEmpty() } ?: emptyList()

    fun getQueryParameter(key: String): String? {
        val q = query ?: return null
        return q.split('&').firstNotNullOfOrNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) null
            else if (pair.substring(0, idx) == key) pair.substring(idx + 1) else null
        }
    }

    override fun toString(): String = raw
    override fun equals(other: Any?): Boolean = other is Uri && other.raw == raw
    override fun hashCode(): Int = raw.hashCode()

    companion object {
        @JvmStatic
        fun parse(uriString: String): Uri = Uri(uriString)

        @JvmStatic
        fun fromFile(file: File): Uri = Uri(file.toURI().toString())

        @JvmStatic
        fun fromParts(scheme: String?, authority: String?, path: String?): Uri =
            Uri(buildString {
                if (scheme != null) append(scheme).append("://")
                if (authority != null) append(authority)
                if (path != null) append(path)
            })

        @JvmStatic
        fun withAppendedPath(base: Uri, pathSegment: String): Uri =
            Uri(base.raw.trimEnd('/') + "/" + pathSegment.trimStart('/'))
    }
}