/*
 * Shim: android.provider.* （OpenableColumns / Settings / DocumentsContract ...）
 *
 * 只有上传模块与权限判断会碰到。桌面端上传走普通文件流，
 * 这些常量与工具保留即可。
 */
package android.provider

object OpenableColumns {
    const val DISPLAY_NAME = "_display_name"
    const val SIZE = "_size"
}

object Settings {
    object Secure {
        fun getString(resolver: android.content.ContentResolver?, name: String?): String? = null
    }

    object Global {
        fun getString(resolver: android.content.ContentResolver?, name: String?): String? = null
    }

    @JvmStatic
    fun canDrawOverlays(context: android.content.Context?): Boolean = true
}

object DocumentsContract {
    @JvmStatic
    fun getTreeDocumentId(uri: android.net.Uri?): String? = null

    @JvmStatic
    fun getDocumentId(uri: android.net.Uri?): String? = null

    @JvmStatic
    fun buildDocumentUri(authority: String?, documentId: String?): android.net.Uri =
        android.net.Uri.parse("content://$authority/document/$documentId")

    @JvmStatic
    fun isTreeUri(uri: android.net.Uri?): Boolean = false
}

object MediaStore {
    object MediaColumns {
        const val DATA = "_data"
        const val DISPLAY_NAME = "_display_name"
        const val SIZE = "_size"
        const val MIME_TYPE = "mime_type"
        const val DATE_ADDED = "date_added"
        const val RELATIVE_PATH = "relative_path"
        const val IS_PENDING = "is_pending"
    }

    object Images : MediaColumns {
        @JvmField
        val EXTERNAL_CONTENT_URI: android.net.Uri =
            android.net.Uri.parse("content://media/external/images/media")
    }

    object Video : MediaColumns {
        @JvmField
        val EXTERNAL_CONTENT_URI: android.net.Uri =
            android.net.Uri.parse("content://media/external/video/media")
    }

    object Audio : MediaColumns {
        @JvmField
        val EXTERNAL_CONTENT_URI: android.net.Uri =
            android.net.Uri.parse("content://media/external/audio/media")
    }

    object Downloads : MediaColumns {
        @JvmField
        val EXTERNAL_CONTENT_URI: android.net.Uri =
            android.net.Uri.parse("content://media/external/downloads")
    }
}