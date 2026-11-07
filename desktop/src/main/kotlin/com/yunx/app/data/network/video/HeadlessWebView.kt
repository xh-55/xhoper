/*
 * 桌面端的 HeadlessWebView 替代实现。
 *
 * 原实现的用途：抖音 / 迅雷网页登录等场景需要跑 JS 或过验证，
 * Android 端用真实 WebView 加载页面后用 evaluateJavascript 取结果。
 *
 * 桌面端没有 WebView（JCEF 打包体积大、启动慢），这里提供同名 API，
 * 但一律返回 null —— 调用方（DouyinParser、XunleiAccountRepository）
 * 都按「拿不到就走备用路径」处理，属于优雅降级：
 *   - 抖音：退回纯 HTTP 接口解析（多数作品仍然可用）
 *   - 迅雷：提示用户手动粘贴 token（原代码本就支持这条兜底路径）
 *
 * 保留这个类（而不是把调用点删掉）是为了让 Android 源码一行不改。
 */
package com.yunx.app.data.network.video

import android.app.Application

object HeadlessWebView {

    /** Android 端用于跟踪 Activity 生命周期；桌面端无意义 */
    fun install(app: Application) = Unit

    /**
     * 加载 [url] 并按 [readyCheckJs] 轮询，就绪后执行 [extractJs]。
     *
     * 桌面端恒返回 null，调用方会走自己的兜底分支。
     * 参数签名与 Android 原实现保持一致，保证调用点零改动。
     */
    suspend fun loadAndExtract(
        url: String,
        userAgent: String,
        readyCheckJs: String,
        extractJs: String,
        timeoutMs: Long = 25_000L,
        pollIntervalMs: Long = 350L,
        extraDocumentStartJs: List<String> = emptyList()
    ): String? {
        android.util.Log.w(
            "HeadlessWebView",
            "桌面端不支持 WebView，跳过 $url（调用方将走兜底路径）"
        )
        return null
    }
}