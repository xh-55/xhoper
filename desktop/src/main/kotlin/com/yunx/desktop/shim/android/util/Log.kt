/*
 * Shim: android.util.Log -> JVM 标准输出。
 *
 * 作用：让原 Android 源码里的 Log.d/i/w/e 在桌面端原样编译、原样输出，
 * 不需要改动业务代码一行。日志同时进内存环形缓冲，供桌面端「日志」面板查看。
 */
package android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    /** 桌面端是否把日志打到 stdout（默认关闭，只留内存缓冲，避免刷屏） */
    @Volatile
    var echoToStdout: Boolean = false

    /** 内存环形缓冲，桌面端「日志」页读取 */
    private const val CAPACITY = 2000
    private val ring = ArrayDeque<String>(CAPACITY)
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun v(tag: String?, msg: String?): Int = log(VERBOSE, tag, msg, null)
    fun v(tag: String?, msg: String?, tr: Throwable?): Int = log(VERBOSE, tag, msg, tr)
    fun d(tag: String?, msg: String?): Int = log(DEBUG, tag, msg, null)
    fun d(tag: String?, msg: String?, tr: Throwable?): Int = log(DEBUG, tag, msg, tr)
    fun i(tag: String?, msg: String?): Int = log(INFO, tag, msg, null)
    fun i(tag: String?, msg: String?, tr: Throwable?): Int = log(INFO, tag, msg, tr)
    fun w(tag: String?, msg: String?): Int = log(WARN, tag, msg, null)
    fun w(tag: String?, msg: String?, tr: Throwable?): Int = log(WARN, tag, msg, tr)
    fun e(tag: String?, msg: String?): Int = log(ERROR, tag, msg, null)
    fun e(tag: String?, msg: String?, tr: Throwable?): Int = log(ERROR, tag, msg, tr)
    fun wtf(tag: String?, msg: String?): Int = log(ASSERT, tag, msg, null)
    fun wtf(tag: String?, msg: String?, tr: Throwable?): Int = log(ASSERT, tag, msg, tr)

    fun isLoggable(tag: String?, level: Int): Boolean = level >= DEBUG

    fun println(priority: Int, tag: String?, msg: String?): Int = log(priority, tag, msg, null)

    fun getStackTraceString(tr: Throwable?): String =
        if (tr == null) "" else tr.stackTraceToString()

    private fun log(priority: Int, tag: String?, msg: String?, tr: Throwable?): Int {
        val line = buildString {
            append(timeFormat.format(Date()))
            append(' ')
            append(priorityLetter(priority))
            append('/')
            append(tag ?: "YunX")
            append(": ")
            append(msg ?: "")
            if (tr != null) {
                append('\n')
                append(tr.stackTraceToString())
            }
        }
        synchronized(lock) {
            if (ring.size >= CAPACITY) ring.removeFirst()
            ring.addLast(line)
        }
        if (echoToStdout) System.out.println(line)
        return 0
    }

    private fun priorityLetter(priority: Int): Char = when (priority) {
        VERBOSE -> 'V'
        DEBUG -> 'D'
        INFO -> 'I'
        WARN -> 'W'
        ERROR -> 'E'
        ASSERT -> 'A'
        else -> '?'
    }

    /** 取最近 [limit] 条日志（桌面端日志面板用） */
    fun snapshot(limit: Int = 500): List<String> = synchronized(lock) {
        if (ring.size <= limit) ring.toList() else ring.toList().takeLast(limit)
    }

    fun clear() = synchronized(lock) { ring.clear() }
}
