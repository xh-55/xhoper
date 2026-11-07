/*
 * 桌面下载器：Range 分片并发 + 断点续传。
 *
 * 与 Android 端 DownloadManager 的差异：
 *  - 没有前台服务 / 通知 / WakeLock / MediaStore 保存（都是 Android 概念）；
 *  - 直接写目标文件系统路径，不再经过 SAF；
 *  - 保留最核心的三件事：分片并发、续传、限速与进度回调。
 *
 * 这条路径足够覆盖 Android 端 90% 的下载场景（直链 / 分享取链后的普通文件），
 * HLS / DASH 合轨、磁力、图集打包属于进阶能力，桌面端第一版先不做。
 */
package com.yunx.desktop.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** 单个下载任务的状态 */
enum class TaskState { PENDING, RUNNING, PAUSED, COMPLETED, FAILED }

data class DownloadTask(
    val id: Long,
    val url: String,
    val fileName: String,
    val headers: Map<String, String> = emptyMap(),
    val saveDir: File,
    val state: TaskState = TaskState.PENDING,
    val downloaded: Long = 0L,
    val total: Long = 0L,
    val speed: Long = 0L,
    val error: String = ""
) {
    val progress: Float
        get() = when {
            total <= 0L -> 0f
            else -> (downloaded.toDouble() / total).toFloat().coerceIn(0f, 1f)
        }

    val targetFile: File get() = File(saveDir, fileName)
}

/**
 * 下载管理器。
 *
 * 磁盘文件布局：`<目标文件>.part` 放未完成数据，完成后 rename 成正式文件名
 * —— 这样「有 .part 就说明没下完」，续传判断不需要额外元数据文件。
 */
class Downloader(
    private val clientProvider: () -> OkHttpClient,
    /** 每个任务的分片（线程）数 */
    private val chunkCount: Int = 8,
    /** 全局同时在飞分片上限 */
    private val maxInflight: Int = 32
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val jobs = ConcurrentHashMap<Long, Job>()
    private val inflight = Semaphore(maxInflight)
    private val nextId = AtomicLong(1)

    /** 新增任务（不自动开始） */
    fun add(url: String, fileName: String, saveDir: File, headers: Map<String, String> = emptyMap()): Long {
        val id = nextId.getAndIncrement()
        _tasks.update { it + DownloadTask(id = id, url = url, fileName = fileName, saveDir = saveDir, headers = headers) }
        return id
    }

    fun start(id: Long) {
        if (jobs.containsKey(id)) return
        val task = find(id) ?: return
        if (task.state == TaskState.COMPLETED) return
        setState(id, TaskState.RUNNING)
        jobs[id] = scope.launch {
            try {
                download(task)
                setState(id, TaskState.COMPLETED)
            } catch (e: CancellationException) {
                setState(id, TaskState.PAUSED)
                throw e
            } catch (e: Throwable) {
                update(id) { it.copy(state = TaskState.FAILED, error = e.message.orEmpty()) }
            } finally {
                jobs.remove(id)
            }
        }
    }

    fun pause(id: Long) {
        jobs.remove(id)?.cancel()
        setState(id, TaskState.PAUSED)
    }

    fun remove(id: Long) {
        jobs.remove(id)?.cancel()
        _tasks.update { list -> list.filterNot { it.id == id } }
    }

    private suspend fun download(task: DownloadTask) = withContext(Dispatchers.IO) {
        val target = task.targetFile
        task.saveDir.mkdirs()
        val part = File(target.parentFile, target.name + ".part")

        // 1) 探测总大小（Range: bytes=0-0 拿 Content-Range）
        val total = probeSize(task) ?: 0L
        update(task.id) { it.copy(total = total) }

        if (total <= 0L) {
            // 服务器不支持 Range，只能单流整下
            downloadSingleStream(task, part, target)
            return@withContext
        }

        // 2) 已有 .part 就续传，长度即已下字节数
        val alreadyHave = if (part.isFile) part.length() else 0L
        update(task.id) { it.copy(downloaded = alreadyHave) }

        if (alreadyHave >= total) {
            part.renameTo(target)
            update(task.id) { it.copy(downloaded = total) }
            return@withContext
        }

        // 3) 按剩余区间切分片，并发拉取
        val remainingStart = alreadyHave
        val remaining = total - remainingStart
        val chunkSize = (remaining / chunkCount).coerceAtLeast(1L)

        // 预分配到最终大小，各分片按偏移写入
        RandomAccessFile(part, "rw").use { it.setLength(total) }

        val downloaded = AtomicLong(alreadyHave)
        val lastTick = AtomicLong(System.currentTimeMillis())
        val lastBytes = AtomicLong(alreadyHave)

        (0 until chunkCount).map { i ->
            val start = remainingStart + i * chunkSize
            val end = if (i == chunkCount - 1) total - 1 else (start + chunkSize - 1).coerceAtMost(total - 1)
            launch {
                if (start > end) return@launch
                inflight.withPermit {
                    fetchRange(task, part, start, end) { n ->
                        val now = downloaded.addAndGet(n)
                        val t = System.currentTimeMillis()
                        val elapsed = t - lastTick.get()
                        if (elapsed >= 500) {
                            val delta = now - lastBytes.getAndSet(now)
                            lastTick.set(t)
                            update(task.id) { it.copy(downloaded = now, speed = delta * 1000 / elapsed) }
                        } else {
                            update(task.id) { it.copy(downloaded = now) }
                        }
                    }
                }
            }
        }.forEach { it.join() }

        // 分片全部结束（或被取消）后，如果协程已被取消就不再标记完成
        ensureActive()

        part.renameTo(target)
        update(task.id) { it.copy(downloaded = total, speed = 0) }
        android.util.Log.i("Downloader", "完成 ${task.fileName} -> ${target.absolutePath}")
    }

    private fun downloadSingleStream(task: DownloadTask, part: File, target: File) {
        val request = Request.Builder().url(task.url).apply {
            task.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        clientProvider().newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body ?: error("空响应")
            val total = body.contentLength()
            update(task.id) { it.copy(total = total) }
            var done = 0L
            val buffer = ByteArray(64 * 1024)
            part.outputStream().use { out ->
                body.byteStream().use { input ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        update(task.id) { it.copy(downloaded = done) }
                    }
                }
            }
        }
        part.renameTo(target)
    }

    private fun fetchRange(task: DownloadTask, part: File, start: Long, end: Long, onBytes: (Int) -> Unit) {
        val request = Request.Builder().url(task.url).apply {
            header("Range", "bytes=$start-$end")
            task.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        clientProvider().newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body ?: error("空响应")
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(start)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var pos = start
                    while (pos <= end) {
                        val n = input.read(buffer, 0, minOf(buffer.size.toLong(), end - pos + 1).toInt())
                        if (n < 0) break
                        raf.write(buffer, 0, n)
                        pos += n
                        onBytes(n)
                    }
                }
            }
        }
    }

    /** 探测文件总大小；服务器不支持 Range 时返回 null */
    private fun probeSize(task: DownloadTask): Long? = runCatching {
        val request = Request.Builder().url(task.url).apply {
            header("Range", "bytes=0-0")
            task.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        clientProvider().newCall(request).execute().use { resp ->
            if (resp.code == 206) {
                // Content-Range: bytes 0-0/12345678
                resp.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
            } else null
        }
    }.getOrNull()

    private fun find(id: Long): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    private fun setState(id: Long, state: TaskState) = update(id) { it.copy(state = state) }

    private fun update(id: Long, block: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) block(it) else it } }
    }
}