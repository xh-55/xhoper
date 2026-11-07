/*
 * 桌面端业务中枢：把 Android 工程里现成的解析实现串起来。
 *
 * 复用（一行未改的原代码）：
 *   ShareLinkParser      —— 从分享文案里认出平台、shareId、提取码
 *   QuarkApi / UCApi / ... —— 各网盘的真实接口实现
 *   QuarkResolveRepository 等 —— token → 列目录 → 取直链 的流程
 *
 * 桌面端自己实现：
 *   Cookie 存储（原来在 Room 里）
 *   下载（原来是 DownloadManager + 前台服务）
 */
package com.yunx.desktop.core

import com.yunx.app.data.network.QuarkApi
import com.yunx.app.data.network.ShareLinkParser
import com.yunx.app.data.network.SharePlatform
import com.yunx.app.data.network.model.ShareFile
import com.yunx.app.data.network.model.ShareSession
import com.yunx.app.data.repository.QuarkResolveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 解析出来的一个可下载条目 */
data class RemoteEntry(
    val file: ShareFile,
    /** 取直链时需要带上的 Cookie / 游客令牌 */
    val extraHeaders: Map<String, String> = emptyMap()
)

/**
 * 应用级单例：持有 HTTP 客户端、Cookie、下载器。
 *
 * Cookie 按平台存成普通文本文件（`files/cookies/<platform>.txt`），
 * 桌面端没有 Keystore，不做加密——这一点与 Android 端「凭证不加密」的现状一致。
 */
class DesktopApp {

    private val cookieDir = File(DesktopContext.filesDir, "cookies").apply { mkdirs() }

    val downloader = Downloader(clientProvider = { HttpClientsRef.download() })

    private val quarkApi by lazy { QuarkApi { HttpClientsRef.api() } }
    private val quarkRepo by lazy { QuarkResolveRepository(quarkApi) }

    /** 当前解析会话（一次粘贴对应一个会话） */
    var session: ShareSession? = null
        private set
    var platform: SharePlatform? = null
        private set
    var currentDir: String = ""
        private set

    /** 读取某平台已保存的 Cookie（没有则空串） */
    fun cookie(platform: SharePlatform): String =
        File(cookieDir, "${platform.name.lowercase()}.txt")
            .takeIf { it.isFile }
            ?.readText()?.trim().orEmpty()

    fun saveCookie(platform: SharePlatform, cookie: String) {
        File(cookieDir, "${platform.name.lowercase()}.txt").writeText(cookie.trim())
    }

    /** 步骤一：识别链接 + 建立会话 */
    suspend fun resolveLink(link: String, pwd: String?): Result<ShareSession> = withContext(Dispatchers.IO) {
        val parsed = ShareLinkParser.parse(link)
            ?: return@withContext Result.failure(IllegalArgumentException("识别不了这个链接，请检查是否完整"))
        platform = parsed.platform
        val cookie = cookie(parsed.platform)
        val result = when (parsed.platform) {
            SharePlatform.QUARK -> quarkRepo.createSession(link, pwd ?: parsed.pwd, cookie)
            else -> Result.failure(
                IllegalStateException("桌面版目前支持：夸克网盘。${parsed.platform.displayName} 的接口已保留，尚未接入界面。")
            )
        }
        result.onSuccess {
            session = it
            // 分享里的根目录 fid 用 "0"
            currentDir = "0"
        }
        result
    }

    /** 步骤二：列出目录 */
    suspend fun listFiles(dirFid: String = currentDir): Result<List<RemoteEntry>> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext Result.failure(IllegalStateException("请先解析链接"))
        val cookie = cookie(SharePlatform.QUARK)
        quarkRepo.listFiles(s, dirFid, cookie).map { list ->
            currentDir = dirFid
            list.map { RemoteEntry(it) }
        }
    }

    /** 步骤三：取直链并入队下载 */
    suspend fun enqueueDownload(entry: RemoteEntry): Result<File> = withContext(Dispatchers.IO) {
        val s = session ?: return@withContext Result.failure(IllegalStateException("请先解析链接"))
        if (entry.file.isdir) return@withContext Result.failure(IllegalArgumentException("暂不支持下载整个文件夹"))

        val cookie = cookie(SharePlatform.QUARK)
        // 优先走「免转存取链」：不在用户网盘留临时目录
        quarkRepo.getShareDownloadLinkWithoutSave(s, entry.file, cookie).mapCatching { link ->
            val id = downloader.add(
                url = link.downloadUrl,
                fileName = link.filename.ifBlank { entry.file.fname },
                saveDir = DesktopContext.downloadDir,
                headers = buildMap {
                    put("User-Agent", com.yunx.app.data.network.QuarkConstants.API_USER_AGENT)
                    if (cookie.isNotBlank()) put("Cookie", cookie)
                    if (link.guestCookie.isNotBlank()) put("Cookie", link.guestCookie)
                }
            )
            downloader.start(id)
            File(DesktopContext.downloadDir, link.filename.ifBlank { entry.file.fname })
        }
    }
}

/**
 * HTTP 客户端统一入口。
 *
 * 直接复用原工程的 HttpClients：它已经按 Android 端调好了超时、并发与 HTTP/1.1 策略，
 * 桌面端没必要重调一遍。
 */
object HttpClientsRef {
    fun api(): okhttp3.OkHttpClient = com.yunx.app.data.network.HttpClients.apiClient()
    fun download(): okhttp3.OkHttpClient = com.yunx.app.data.network.HttpClients.downloadClient()
}