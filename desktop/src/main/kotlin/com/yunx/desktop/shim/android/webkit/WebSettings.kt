/*
 * Shim: android.webkit.WebSettings（最小可编译壳）
 *
 * 只有 XunleiWebCredential.applyDesktopClientHints(settings) 用到它，
 * 而那一步是「给 WebView 设置 UA / client hints」。桌面端没有 WebView，
 * 且 XunleiWebCredential 的 DESKTOP_UA 常量本身就是完整桌面 UA 字符串，
 * 所以这里只需要让 setUserAgentString / userAgentString 存在即可。
 */
package android.webkit

open class WebSettings {
    var userAgentString: String? = null
    var javaScriptEnabled: Boolean = true
    var domStorageEnabled: Boolean = true
    var databaseEnabled: Boolean = false
    var loadsImagesAutomatically: Boolean = true
    var useWideViewPort: Boolean = true
    var loadWithOverviewMode: Boolean = true
    var supportZoom: Boolean = true
    var builtInZoomControls: Boolean = false
    var displayZoomControls: Boolean = false
    var mediaPlaybackRequiresUserGesture: Boolean = false
    var mixedContentMode: Int = 0
    var cacheMode: Int = 0
    var textZoom: Int = 100

    companion object {
        const val MIXED_CONTENT_ALWAYS_ALLOW = 0
        const val MIXED_CONTENT_NEVER_ALLOW = 1
        const val MIXED_CONTENT_COMPATIBILITY_MODE = 2
        const val LOAD_DEFAULT = -1
        const val LOAD_NO_CACHE = 2
    }
}