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
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 自建更新服务器（XhOper）。
 *
 * 协议：`GET {BASE_URL}/api/v1/app/latest`
 *
 * 成功（HTTP 200）：
 * ```
 * {
 *   "code": 0,
 *   "message": "ok",
 *   "data": {
 *     "versionName": "1.1",
 *     "versionCode": 2,
 *     "notes": "更新说明（纯文本）",
 *     "apkName": "XhOper-1.1.apk",
 *     "apkUrl": "http://…/apk/XhOper-1.1.apk",
 *     "size": 32600027,
 *     "sha256": "…",
 *     "publishedAt": "2026-11-07T10:00:00Z"
 *   }
 * }
 * ```
 * 没有新版本时**返回 HTTP 404**（或 `code` 非 0），客户端据此提示「已是最新版本」。
 *
 * 设计要点：
 * 1. 只此一处实现应用自身的更新检查；原 GitHub Release 通道（[UpdateChecker.fetchLatestRelease]）
 *    保留给 Gopeed 内核等第三方仓库使用，不再承载本应用的更新。
 * 2. 所有失败都转成 `CheckResult.Failure`，不抛异常，原因可直接展示给用户。
 * 3. 返回结构复用 [UpdateChecker.Release] / [UpdateChecker.CheckResult]，
 *    这样界面层（更新弹窗、设置页手动检查）无需改动数据结构。
 */
object AppUpdateServer {

    /**
     * 自建服务器地址（与公告服务同一台机器，见 `AnnouncementApi.BASE_URL`）。
     *
     * ★ 当前部署是 HTTP 明文，`network_security_config.xml` 里为该主机单独开了明文例外，
     *   两处必须一起改：后端上 HTTPS 后把这里改 `https://` 并删掉那条 domain-config。
     */
    const val BASE_URL = "http://43.128.136.224"

    /** 更新检查日志标签 */
    private const val TAG = "XhOper-Update"

    /** 更新清单路径 */
    private const val LATEST_PATH = "/api/v1/app/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 拉取最新版本信息。
     * [includePrerelease] 仅为接口兼容保留（自建服务器只有一个通道），不参与判断。
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun fetchLatest(includePrerelease: Boolean = false): UpdateChecker.CheckResult =
        withContext(Dispatchers.IO) {
            runCatching { requestLatest() }
                .onFailure { e ->
                    Log.e(TAG, "获取更新信息异常：${e.javaClass.simpleName}: ${e.message}", e)
                }
                .getOrElse { e ->
                    UpdateChecker.CheckResult.Failure(
                        "${e.javaClass.simpleName}: ${e.message ?: "未知错误"}"
                    )
                }
        }

    private fun requestLatest(): UpdateChecker.CheckResult {
        val url = BASE_URL + LATEST_PATH
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            if (resp.code == 404) {
                Log.i(TAG, "服务器没有发布新版本（HTTP 404）")
                return UpdateChecker.CheckResult.Failure("服务器暂无更新")
            }
            if (!resp.isSuccessful) {
                Log.e(TAG, "获取更新信息失败：HTTP ${resp.code}（$url）")
                return UpdateChecker.CheckResult.Failure("HTTP ${resp.code}")
            }
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) {
                Log.e(TAG, "更新接口响应为空（$url）")
                return UpdateChecker.CheckResult.Failure("服务器响应为空")
            }
            val json = try {
                JSONObject(text)
            } catch (e: Exception) {
                Log.e(TAG, "更新接口响应不是合法 JSON（前 200 字=${text.take(200)}）", e)
                return UpdateChecker.CheckResult.Failure("响应格式异常")
            }
            val code = json.optInt("code", 0)
            if (code != 0) {
                val message = json.optString("message").ifBlank { "服务器返回错误码 $code" }
                Log.e(TAG, "更新接口业务失败：[$code] $message")
                return UpdateChecker.CheckResult.Failure(message)
            }
            val data = json.optJSONObject("data")
                ?: return UpdateChecker.CheckResult.Failure("响应缺少 data 节点")
            val versionName = data.optString("versionName").trim().trimStart('v', 'V')
            if (versionName.isBlank()) {
                Log.e(TAG, "更新信息缺少 versionName")
                return UpdateChecker.CheckResult.Failure("缺少版本号")
            }
            val apkUrl = data.optString("apkUrl").trim()
            if (apkUrl.isBlank()) {
                Log.e(TAG, "更新信息缺少 apkUrl")
                return UpdateChecker.CheckResult.Failure("缺少下载地址")
            }
            val rawName = data.optString("apkName").trim()
                .ifBlank { "XhOper-$versionName.apk" }
            val apkName = if (rawName.endsWith(".apk", true)) rawName else "$rawName.apk"
            val asset = UpdateChecker.Asset(
                name = apkName,
                downloadUrl = apkUrl,
                size = data.optLong("size"),
                digest = data.optString("sha256")
            )
            val notes = data.optString("notes").ifBlank { "暂无更新说明" }
            Log.d(TAG, "获取更新信息成功：v$versionName（$apkName，${asset.size} 字节）")
            return UpdateChecker.CheckResult.Success(
                UpdateChecker.Release(
                    tagName = "v$versionName",
                    body = notes,
                    assets = listOf(asset),
                    publishedAt = data.optString("publishedAt"),
                    htmlUrl = data.optString("htmlUrl"),
                    prerelease = false
                )
            )
        }
    }
}
