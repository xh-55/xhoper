/*
 * Shim: android.content.ContentResolver / Intent / ComponentName
 *
 * 桌面端没有 Binder/Provider 体系。这里提供：
 *  - ContentResolver.openFileDescriptor / openInputStream：把 file:// Uri 映射到真实文件；
 *  - Intent：只存 action/data，供业务代码构造后传给空实现；
 *  - ComponentName：字符串壳。
 *
 * 上传功能在桌面端不启用（UI 不暴露），这些方法被调用到的情况基本不会发生，
 * 但保留实现可避免业务代码编译期缺失。
 */
package android.content

import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ContentResolver {
    fun openInputStream(uri: Uri): InputStream? = runCatching {
        val f = toFile(uri) ?: return null
        FileInputStream(f)
    }.getOrNull()

    fun openFileDescriptor(uri: Uri, mode: String): ParcelFileDescriptor? = runCatching {
        val f = toFile(uri) ?: return null
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }.getOrNull()

    fun getType(uri: Uri): String? = null

    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): android.database.Cursor? = null

    fun insert(uri: Uri, values: ContentValues?): Uri? = null
    fun delete(uri: Uri, where: String?, args: Array<String>?): Int = 0
    fun update(uri: Uri, values: ContentValues?, where: String?, args: Array<String>?): Int = 0

    /** 把 file:// / 裸路径 Uri 还原成 File */
    fun toFile(uri: Uri): File? = runCatching {
        val s = uri.toString()
        when {
            s.startsWith("file://") -> File(java.net.URI(s))
            s.startsWith("/") -> File(s)
            else -> null
        }
    }.getOrNull()
}

class Intent {
    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var flags: Int = 0
    private val extras = mutableMapOf<String, Any?>()

    constructor()
    constructor(action: String?) { this.action = action }
    constructor(action: String?, uri: Uri?) {
        this.action = action
        this.data = uri
    }

    fun setAction(a: String?): Intent = apply { action = a }
    fun setData(u: Uri?): Intent = apply { data = u }
    fun setType(t: String?): Intent = apply { type = t }
    fun addFlags(f: Int): Intent = apply { flags = flags or f }
    fun putExtra(key: String, value: Any?): Intent = apply { extras[key] = value }
    fun getStringExtra(key: String): String? = extras[key] as? String
    fun getIntExtra(key: String, def: Int): Int = extras[key] as? Int ?: def
    fun getBooleanExtra(key: String, def: Boolean): Boolean = extras[key] as? Boolean ?: def

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_SENDTO = "android.intent.action.SENDTO"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
    }
}

class ComponentName(val packageName: String, val className: String)

/** ContentValues：桌面端只做字符串桶 */
class ContentValues {
    private val map = LinkedHashMap<String, Any?>()
    fun put(key: String, value: String?) { map[key] = value }
    fun put(key: String, value: Int?) { map[key] = value }
    fun put(key: String, value: Long?) { map[key] = value }
    fun put(key: String, value: Boolean?) { map[key] = value }
    fun getAsString(key: String): String? = map[key]?.toString()
    fun size(): Int = map.size
    fun keySet(): Set<String> = map.keys
}

/** ParcelFileDescriptor：包一层 File，AutoCloseInputStream 供 UploadSource 用 */
class ParcelFileDescriptor(private val file: File) {
    fun getFileDescriptor(): java.io.FileDescriptor? = null
    fun close() = Unit

    class AutoCloseInputStream(private val pfd: ParcelFileDescriptor) : InputStream() {
        private val delegate: InputStream? = runCatching { FileInputStream(pfd.file) }.getOrNull()
        val channel: java.nio.channels.FileChannel?
            get() = (delegate as? FileInputStream)?.channel

        override fun read(): Int = delegate?.read() ?: -1
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate?.read(b, off, len) ?: -1
        override fun close() {
            runCatching { delegate?.close() }
            pfd.close()
        }
    }

    companion object {
        const val MODE_READ_ONLY = 0x10000000
        @JvmStatic
        fun open(file: File, mode: Int): ParcelFileDescriptor = ParcelFileDescriptor(file)
    }
}
