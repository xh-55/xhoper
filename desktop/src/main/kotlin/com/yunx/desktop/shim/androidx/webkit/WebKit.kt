/*
 * Shim: androidx.webkit.*
 *
 * 只有 XunleiWebCredential.applyDesktopClientHints 用，作用是让 WebView 的
 * client hints 与桌面 UA 对齐。桌面端没有 WebView，功能直接 no-op，
 * 但类型与方法签名必须齐全，否则原代码编不过。
 */
package androidx.webkit

object WebViewFeature {
    const val USER_AGENT_METADATA = "USER_AGENT_METADATA"
    const val ALGORITHMIC_DARKENING = "ALGORITHMIC_DARKENING"
    const val FORCE_DARK = "FORCE_DARK"
    const val MULTI_PROCESS = "MULTI_PROCESS"

    @JvmStatic
    fun isFeatureSupported(feature: String?): Boolean = false
}

/**
 * client hints 元数据。字段与方法名严格对齐 androidx.webkit 的同名类，
 * 因为原代码是 Builder 链式构造（setBrandVersionList / setFullVersion / ...）。
 */
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
        private var brandVersionList: List<BrandVersion> = emptyList()
        private var fullVersion: String = ""
        private var platform: String = ""
        private var platformVersion: String = ""
        private var architecture: String = ""
        private var model: String = ""
        private var mobile: Boolean = false
        private var wow64: Boolean = false
        private var bitness: Int = 64

        fun setBrandVersionList(list: List<BrandVersion>) = apply { brandVersionList = list }
        fun setFullVersion(v: String) = apply { fullVersion = v }
        fun setPlatform(v: String) = apply { platform = v }
        fun setPlatformVersion(v: String) = apply { platformVersion = v }
        fun setArchitecture(v: String) = apply { architecture = v }
        fun setModel(v: String) = apply { model = v }
        fun setMobile(v: Boolean) = apply { mobile = v }
        fun setWow64(v: Boolean) = apply { wow64 = v }
        fun setBitness(v: Int) = apply { bitness = v }

        fun build(): UserAgentMetadata = UserAgentMetadata()
    }
}

object WebSettingsCompat {
    @JvmStatic
    fun setUserAgentMetadata(settings: android.webkit.WebSettings?, metadata: UserAgentMetadata?) = Unit

    @JvmStatic
    fun setAlgorithmicDarkeningAllowed(settings: android.webkit.WebSettings?, allow: Boolean) = Unit

    @JvmStatic
    fun setForceDark(settings: android.webkit.WebSettings?, forceDark: Int) = Unit
}

object WebViewCompat {
    @JvmStatic
    fun setWebViewRenderProcessClient(webView: android.webkit.WebView?, client: Any?) = Unit

    @JvmStatic
    fun postWebMessage(webView: android.webkit.WebView?, message: Any?, targetOrigin: Any?) = Unit
}