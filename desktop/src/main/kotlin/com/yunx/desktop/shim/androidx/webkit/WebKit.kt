/*
 * Shim: androidx.webkit.*
 *
 * 只有 XunleiWebCredential.applyDesktopClientHints 用到，作用是让 WebView 的
 * client hints 与桌面 UA 对齐。桌面端没有 WebView，功能直接 no-op。
 */
package androidx.webkit

object WebViewFeature {
    const val USER_AGENT_METADATA = "USER_AGENT_METADATA"
    const val ALGORITHMIC_DARKENING = "ALGORITHMIC_DARKENING"
    const val FORCE_DARK = "FORCE_DARK"

    @JvmStatic
    fun isFeatureSupported(feature: String?): Boolean = false
}

class UserAgentMetadata {
    class BrandVersion(val brand: String, val version: String) {
        class Builder {
            private var brand: String = ""
            private var version: String = ""
            fun setBrand(b: String) = apply { brand = b }
            fun setVersion(v: String) = apply { version = v }
            fun build(): BrandVersion = BrandVersion(brand, version)
        }
    }

    class Builder {
        private var brands: List<BrandVersion> = emptyList()
        private var fullVersion: String = ""
        private var platform: String = ""
        private var platformVersion: String = ""
        private var architecture: String = ""
        private var model: String = ""
        private var mobile: Boolean = false
        private var wow64: Boolean = false

        fun setBrandVersionList(list: List<BrandVersion>) = apply { brands = list }
        fun setFullVersion(v: String) = apply { fullVersion = v }
        fun setPlatform(v: String) = apply { platform = v }
        fun setPlatformVersion(v: String) = apply { platformVersion = v }
        fun setArchitecture(v: String) = apply { architecture = v }
        fun setModel(v: String) = apply { model = v }
        fun setMobile(v: Boolean) = apply { mobile = v }
        fun setWow64(v: Boolean) = apply { wow64 = v }
        fun build(): UserAgentMetadata = UserAgentMetadata()
    }
}

object WebSettingsCompat {
    @JvmStatic
    fun setUserAgentMetadata(settings: android.webkit.WebSettings?, metadata: UserAgentMetadata?) = Unit

    @JvmStatic
    fun setAlgorithmicDarkeningAllowed(settings: android.webkit.WebSettings?, allow: Boolean) = Unit
}

object WebViewCompat {
    @JvmStatic
    fun setWebViewRenderProcessClient(
        webView: android.webkit.WebView?,
        client: Any?
    ) = Unit

    @JvmStatic
    fun postWebMessage(
        webView: android.webkit.WebView?,
        message: Any?,
        targetOrigin: Any?
    ) = Unit
}