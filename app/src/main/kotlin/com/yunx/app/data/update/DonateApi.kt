/*
 * XhOper
 * Copyright (C) 2026 任我行
 *
 * 本文件是「云析 · YunX」(Copyright (C) 2026 CYQawa, AGPL-3.0) 衍生作品的一部分。
 * 该程序是自由软件：你可以依据 GNU Affero 通用公共许可证（AGPL-3.0）的条款
 * 重新发布和/或修改它。完整许可证见仓库根目录的 LICENSE 文件。
 */

package com.yunx.app.data.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 云端「支持开发」收款码。
 *
 * ★ 为什么放服务器而不是打进 APK：
 *   收款码属于运营素材，可能随时更换（换号、换渠道、活动结束后下线）。
 *   打进 APK 意味着每次改图都要重新发版，用户还得再升级一次；放服务器则可以随时替换图片，
 *   客户端每次打开页面拉一次即可，旧版本 App 也能立刻看到新码。
 *
 * 协议：`GET {BASE_URL}/api/v1/donate`
 *
 * 成功（HTTP 200）：
 * ```
 * {
 *   "code": 0,
 *   "message": "ok",
 *   "data": {
 *     "title": "支持开发",
 *     "subtitle": "你的支持，是持续维护与更新的动力",
 *     "desc": "XhOper 完全免费，所有功能无需捐赠即可正常使用……",
 *     "thanks": "感谢每一位支持者 ❤",
 *     "items": [
 *       {
 *         "name": "支付宝",
 *         "role": "作者 任我行",
 *         "imageUrl": "http://…/donate/alipay.png",
 *         "hint": "保存二维码到相册后，打开支付宝「扫一扫」即可支持"
 *       }
 *     ]
 *   }
 * }
 * ```
 *
 * 字段约定：
 * - `items` 为空数组（或缺失）⇒ 客户端显示「暂时未开放」；这比返回 404 更明确，也便于随时上下线。
 * - `subtitle` / `desc` / `thanks` 留空则不渲染对应的版块（页面不会留空行）。
 * - `imageUrl` 支持相对路径（如 `/donate/alipay.png`），会自动拼上 [AppUpdateServer.BASE_URL]。
 * - 接口不可用（网络故障、404/5xx、JSON 解析失败）⇒ [Result.Failure]，页面显示「加载失败 + 重试」。
 */
object DonateApi {

    /** 与更新/公告服务同一台机器 */
    private const val BASE_URL = AppUpdateServer.BASE_URL

    private const val PATH = "/api/v1/donate"

    private const val TAG = "XhOper-Donate"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /** 一个收款渠道 */
    data class DonateItem(
        /** 渠道名：支付宝 / 微信 */
        val name: String,
        /** 图片地址（已补全为绝对 URL） */
        val imageUrl: String,
        /** 角色标签：如「作者 任我行」；留空则不显示 */
        val role: String = "",
        /** 卡片底部的扫码提示；留空则用内置默认文案 */
        val hint: String = ""
    )

    /** 页面数据 */
    data class DonateInfo(
        val title: String,
        val subtitle: String,
        val desc: String,
        val thanks: String,
        val items: List<DonateItem>
    ) {
        /** 没有任何渠道 ⇒ 页面显示「暂未开放」 */
        val isEmpty: Boolean get() = items.isEmpty()
    }

    /** 拉取结果 */
    sealed interface Result {
        data class Success(val info: DonateInfo) : Result

        /** [message] 可直接展示给用户 */
        data class Failure(val message: String) : Result
    }

    /**
     * 拉取收款码列表。
     *
     * 不会抛异常：所有错误都转成 [Result.Failure]。
     */
    suspend fun fetch(): Result = withContext(Dispatchers.IO) {
        val url = BASE_URL + PATH
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                // 加时间戳参数，绕开部分运营商/中间层对 GET 的缓存，保证换码后立刻生效
                .url("$url?_t=${System.currentTimeMillis()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} from $url")
                    return@withContext Result.Failure(
                        if (response.code == 404) "服务端暂未配置" else "服务不可用（HTTP ${response.code}）"
                    )
                }
                if (body.isBlank()) {
                    Log.w(TAG, "empty body from $url")
                    return@withContext Result.Failure("服务器返回为空")
                }

                val info = parse(body)
                    ?: return@withContext Result.Failure("数据格式不正确")

                Result.Success(info)
            }
        } catch (e: Exception) {
            // 网络异常、超时、DNS 失败等
            Log.w(TAG, "fetch failed: ${e.javaClass.simpleName} ${e.message}")
            Result.Failure(friendlyMessage(e))
        }
    }

    /**
     * 解析响应体。返回 null 表示结构不符合约定。
     *
     * 单独抽出来便于单测，也便于以后兼容旧格式。
     */
    internal fun parse(body: String): DonateInfo? {
        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            Log.w(TAG, "not a json object: ${e.message}")
            return null
        }

        // code 非 0 视为业务失败
        val code = root.optInt("code", 0)
        if (code != 0) {
            Log.w(TAG, "server code=$code message=${root.optString("message")}")
            return null
        }

        val data = root.optJSONObject("data") ?: return null

        val items = ArrayList<DonateItem>()
        val array: JSONArray? = data.optJSONArray("items")
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawUrl = obj.optString("imageUrl")
                if (rawUrl.isBlank()) continue
                items += DonateItem(
                    name = obj.optString("name").ifBlank { "收款码" },
                    imageUrl = absoluteUrl(rawUrl),
                    role = obj.optString("role"),
                    hint = obj.optString("hint")
                )
            }
        }

        return DonateInfo(
            title = data.optString("title").ifBlank { "支持开发" },
            subtitle = data.optString("subtitle"),
            desc = data.optString("desc"),
            thanks = data.optString("thanks"),
            items = items
        )
    }

    /** 相对路径补全成绝对地址 */
    private fun absoluteUrl(url: String): String = when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> BASE_URL + url
        else -> "$BASE_URL/$url"
    }

    /** 把底层异常翻译成用户能看懂的话 */
    private fun friendlyMessage(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "无法连接服务器，请检查网络"
        is java.net.SocketTimeoutException -> "连接超时，请稍后重试"
        is javax.net.ssl.SSLException -> "安全连接失败"
        else -> "加载失败：${e.javaClass.simpleName}"
    }
}