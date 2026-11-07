/*
 * Shim: android.webkit.WebView 家族（最小可编译壳）
 *
 * HeadlessWebView 已从桌面编译中排除；这里只保留类型，
 * 防止 XunleiWebCredential 等文件里出现的引用报错。
 */
package android.webkit

import android.content.Context

open class WebView(context: Context?) {
    var settings: WebSettings = WebSettings()
    var webViewClient: WebViewClient? = null
    var webChromeClient: Any? = null

    open fun loadUrl(url: String) = Unit
    open fun loadDataWithBaseURL(baseUrl: String?, data: String?, mimeType: String?, encoding: String?, historyUrl: String?) = Unit
    open fun evaluateJavascript(script: String?, callback: ValueCallback<String>?) = Unit
    open fun postUrl(url: String?, postData: ByteArray?) = Unit
    open fun stopLoading() = Unit
    open fun reload() = Unit
    open fun destroy() = Unit
    open fun setInitialScale(scaleInPercent: Int) = Unit
    open fun addJavascriptInterface(obj: Any?, name: String) = Unit
}

open class WebViewClient {
    open fun onPageStarted(view: WebView?, url: String?, favicon: Any?) = Unit
    open fun onPageFinished(view: WebView?, url: String?) = Unit
    open fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
    open fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) = Unit
}

fun interface ValueCallback<T> {
    fun onReceiveValue(value: T?)
}

class WebResourceRequest
class WebResourceError

open class WebChromeClient
open class CookieManager {
    fun setCookie(url: String?, value: String?) = Unit
    fun getCookie(url: String?): String? = null
    fun removeAllCookies(callback: ValueCallback<Boolean>?) = Unit
    fun flush() = Unit
}