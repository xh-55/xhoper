/*
 * Shim: android.content.SharedPreferences -> Java Properties 落盘。
 *
 * 语义尽量贴近 Android：
 *  - 同一个 name 在同一进程内是单例（业务代码依赖这一点做跨模块共享配置）；
 *  - apply() 异步、commit() 同步，但桌面端都当成「立即写盘 + 更新内存」；
 *  - 值的类型以 getXxx 时的默认值类型为准（Android 亦如此）。
 */
package android.content

import java.io.File
import java.util.Properties

interface SharedPreferences {
    fun getAll(): Map<String, *>
    fun getString(key: String, defValue: String?): String?
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>?
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun contains(key: String): Boolean
    fun edit(): Editor
    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?)
    fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?)

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putStringSet(key: String, values: Set<String>?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun commit(): Boolean
        fun apply()
    }

    fun interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    }
}

/**
 * Properties 后端的实现。
 *
 * 用一个全局目录存 `.properties`，而不是 JSON：Properties 天然处理转义，
 * 且业务代码只会读写标量（String/Int/Long/Boolean/Float），没有嵌套结构。
 */
class PropertiesSharedPreferences(
    private val file: File
) : SharedPreferences {

    private val props = Properties()
    private val lock = Any()
    private val listeners = CopyOnWriteList<SharedPreferences.OnSharedPreferenceChangeListener>()

    init {
        synchronized(lock) {
            runCatching {
                if (file.isFile) file.inputStream().use { props.load(it) }
            }
        }
    }

    override fun getAll(): Map<String, *> = synchronized(lock) {
        props.entries.associate { it.key.toString() to it.value }
    }

    override fun getString(key: String, defValue: String?): String? = synchronized(lock) {
        (props[key] as? String) ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = synchronized(lock) {
        val raw = props[key] as? String ?: return@synchronized defValues
        // 用 NUL 分隔的字符串承载 Set（值里出现 NUL 的概率可忽略）
        raw.split('\u0000').filter { it.isNotEmpty() }.toSet()
    }

    override fun getInt(key: String, defValue: Int): Int = synchronized(lock) {
        (props[key] as? String)?.toIntOrNull() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long = synchronized(lock) {
        (props[key] as? String)?.toLongOrNull() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float = synchronized(lock) {
        (props[key] as? String)?.toFloatOrNull() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean = synchronized(lock) {
        (props[key] as? String)?.toBooleanStrictOrNull() ?: defValue
    }

    override fun contains(key: String): Boolean = synchronized(lock) { props.containsKey(key) }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners.remove(listener)
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val changes = LinkedHashMap<String, String?>()
        private var clearFirst = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            changes[key] = value
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply {
            changes[key] = values?.joinToString("\u0000")
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            changes[key] = value.toString()
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            changes[key] = value.toString()
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            changes[key] = value.toString()
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            changes[key] = value.toString()
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            changes[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearFirst = true
            changes.clear()
        }

        override fun commit(): Boolean = commitInternal()

        override fun apply() {
            commitInternal()
        }

        private fun commitInternal(): Boolean = synchronized(lock) {
            runCatching {
                if (clearFirst) props.clear()
                for ((k, v) in changes) {
                    if (v == null) props.remove(k) else props[k] = v
                }
                file.parentFile?.mkdirs()
                file.outputStream().use { props.store(it, null) }
            }.onFailure {
                // 写盘失败不该让业务崩，Android 的 apply 也是静默的
                android.util.Log.w("Prefs", "写入 ${file.name} 失败: ${it.message}")
            }
            changes.keys.forEach { key -> listeners.forEach { it.onSharedPreferenceChanged(this@PropertiesSharedPreferences, key) } }
            changes.clear()
            clearFirst = false
            true
        }
    }
}

/** 极简线程安全列表，避免依赖 kotlinx 的并发集合 */
private class CopyOnWriteList<T> {
    @Volatile
    private var items: List<T> = emptyList()

    fun add(item: T) {
        synchronized(this) { items = items + item }
    }

    fun remove(item: T) {
        synchronized(this) { items = items - item }
    }

    fun forEach(action: (T) -> Unit) {
        items.forEach(action)
    }
}
