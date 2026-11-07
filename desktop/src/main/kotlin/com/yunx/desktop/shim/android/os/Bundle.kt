/*
 * Shim: android.os.Bundle
 *
 * 业务层只在 ApplicationInfo.metaData、Intent extra 上用到。
 */
package android.os

class Bundle {
    private val map = LinkedHashMap<String, Any?>()

    fun putString(key: String, value: String?) = apply { map[key] = value }
    fun putInt(key: String, value: Int) = apply { map[key] = value }
    fun putLong(key: String, value: Long) = apply { map[key] = value }
    fun putBoolean(key: String, value: Boolean) = apply { map[key] = value }
    fun putParcelable(key: String, value: Any?) = apply { map[key] = value }

    fun getString(key: String): String? = map[key] as? String
    fun getString(key: String, def: String?): String? = (map[key] as? String) ?: def
    fun getInt(key: String): Int = map[key] as? Int ?: 0
    fun getInt(key: String, def: Int): Int = map[key] as? Int ?: def
    fun getLong(key: String): Long = map[key] as? Long ?: 0L
    fun getBoolean(key: String): Boolean = map[key] as? Boolean ?: false
    fun getBoolean(key: String, def: Boolean): Boolean = map[key] as? Boolean ?: def

    fun containsKey(key: String): Boolean = map.containsKey(key)
    fun isEmpty(): Boolean = map.isEmpty()
    fun size(): Int = map.size
    fun keySet(): Set<String> = map.keys
    fun remove(key: String) = apply { map.remove(key) }
    fun clear() = apply { map.clear() }
}