/*
 * Shim: android.util.Base64 -> java.util.Base64。
 *
 * 常量取值与 Android 保持一致（DEFAULT=0, NO_PADDING=1, NO_WRAP=2, ...），
 * 因为业务代码里有 `flags and NO_PADDING != 0` 这类位运算判断。
 * 关键语义：Android 的 DEFAULT 会自动换行（每 76 字符插入 \n），
 * 而 URL_SAFE 用 -_ 替换 +/。
 */
package android.util

import java.util.Base64 as JBase64

object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8
    const val NO_CLOSE = 16

    fun encodeToString(input: ByteArray, flags: Int): String = encodeToString(input, 0, input.size, flags)

    fun encodeToString(input: ByteArray, offset: Int, len: Int, flags: Int): String {
        val slice = if (offset == 0 && len == input.size) input else input.copyOfRange(offset, offset + len)
        val encoder = when {
            (flags and URL_SAFE) != 0 && (flags and NO_PADDING) != 0 ->
                JBase64.getUrlEncoder().withoutPadding()
            (flags and URL_SAFE) != 0 -> JBase64.getUrlEncoder()
            (flags and NO_PADDING) != 0 -> JBase64.getEncoder().withoutPadding()
            else -> JBase64.getEncoder()
        }
        val encoded = encoder.encodeToString(slice)
        // Android 的 DEFAULT / CRLF 会自动折行；NO_WRAP 不折行。
        if ((flags and NO_WRAP) != 0) return encoded
        val lineSep = if ((flags and CRLF) != 0) "\r\n" else "\n"
        return if (encoded.length <= 76) encoded else encoded.chunked(76).joinToString(lineSep)
    }

    fun encode(input: ByteArray, flags: Int): ByteArray = encodeToString(input, flags).toByteArray(Charsets.US_ASCII)

    fun encode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray =
        encodeToString(input, offset, len, flags).toByteArray(Charsets.US_ASCII)

    fun decode(str: String, flags: Int): ByteArray = decode(str, flags, DEFAULT)

    fun decode(str: String, flags: Int, @Suppress("UNUSED_PARAMETER") offset: Int): ByteArray {
        // Android 容忍空白与换行，这里先剔除；也容忍 URL 安全字符。
        val cleaned = buildString(str.length) {
            for (ch in str) {
                when (ch) {
                    ' ', '\n', '\r', '\t' -> Unit
                    else -> append(ch)
                }
            }
        }
        // 兼容缺失 padding
        val padded = when (cleaned.length % 4) {
            2 -> "$cleaned=="
            3 -> "$cleaned="
            else -> cleaned
        }
        val decoder = if ((flags and URL_SAFE) != 0 || padded.contains('-') || padded.contains('_')) {
            JBase64.getUrlDecoder()
        } else {
            JBase64.getDecoder()
        }
        return decoder.decode(padded)
    }

    fun decode(input: ByteArray, flags: Int): ByteArray = decode(String(input, Charsets.US_ASCII), flags)

    fun decode(input: ByteArray, offset: Int, len: Int, flags: Int): ByteArray =
        decode(String(input, offset, len, Charsets.US_ASCII), flags)
}