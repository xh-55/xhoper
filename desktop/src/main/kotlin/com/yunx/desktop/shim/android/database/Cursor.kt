/*
 * Shim: android.database.Cursor（最小可用）
 */
package android.database

interface Cursor : AutoCloseable {
    fun moveToFirst(): Boolean
    fun moveToNext(): Boolean
    fun getCount(): Int
    fun getColumnIndex(name: String): Int
    fun getString(index: Int): String?
    fun getLong(index: Int): Long
    fun getInt(index: Int): Int
    fun close()
}