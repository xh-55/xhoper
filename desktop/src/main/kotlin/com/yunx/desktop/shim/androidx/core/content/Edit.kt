/*
 * Shim: androidx.core.content.edit
 *
 * 原代码写法：prefs.edit { putString(...) }（KTX 扩展，自动 apply）。
 * 桌面端直接复用我们自己的 SharedPreferences.Editor 即可。
 */
package androidx.core.content

import android.content.SharedPreferences

inline fun SharedPreferences.edit(
    commit: Boolean = false,
    action: SharedPreferences.Editor.() -> Unit
) {
    val editor = edit()
    action(editor)
    if (commit) editor.commit() else editor.apply()
}