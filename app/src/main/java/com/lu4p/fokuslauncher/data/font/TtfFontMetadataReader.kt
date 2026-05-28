package com.lu4p.fokuslauncher.data.font

import android.graphics.fonts.Font
import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Reads the typographic family name embedded in a `.ttf` [name] table, with a platform [Font]
 * fast path on API 29+.
 */
object TtfFontMetadataReader {

    private const val TAG_NAME = 0x6E616D65 // "name"

    /** Typographic family (preferred when present). */
    private const val NAME_ID_TYPOGRAPHIC_FAMILY = 16

    /** Legacy font family name. */
    private const val NAME_ID_FONT_FAMILY = 1

    private const val PLATFORM_WINDOWS = 3
    private const val PLATFORM_MAC = 1
    private const val LANG_ENGLISH_US = 0x0409

    fun readFamilyDisplayName(file: File): String? {
        readFamilyNameViaPlatformFont(file)?.let { return it }
        return readFamilyNameFromNameTable(file)
    }

    private fun readFamilyNameViaPlatformFont(file: File): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
                    val font = Font.Builder(file).build()
                    extractFamilyName(font)?.trim()?.takeIf { it.isNotEmpty() }
                }
                .getOrNull()
    }

    private fun extractFamilyName(font: Font): String? =
            runCatching {
                        val m = font.javaClass.getMethod("getFamilyName")
                        m.invoke(font) as? String
                    }
                    .getOrNull()
                    ?: runCatching {
                        val m = font.javaClass.getDeclaredMethod("getFamilyName")
                        m.isAccessible = true
                        m.invoke(font) as? String
                    }
                            .getOrNull()

    private fun readFamilyNameFromNameTable(file: File): String? =
            runCatching {
                        RandomAccessFile(file, "r").use { raf ->
                            val numTables = raf.readUInt16BE(4) ?: return@use null
                            var nameOffset = -1
                            var nameLength = 0
                            for (i in 0 until numTables) {
                                val recordPos = 12L + i * 16L
                                val tag = raf.readIntBE(recordPos) ?: continue
                                if (tag != TAG_NAME) continue
                                nameOffset = raf.readUInt32BE(recordPos + 8)?.toInt() ?: continue
                                nameLength = raf.readUInt32BE(recordPos + 12)?.toInt() ?: continue
                                break
                            }
                            if (nameOffset < 0 || nameLength <= 0) return@use null
                            parseNameTable(raf, nameOffset, nameLength)
                        }
                    }
                    .getOrNull()

    private fun parseNameTable(raf: RandomAccessFile, tableOffset: Int, tableLength: Int): String? {
        if (tableLength < 6) return null
        val base = tableOffset.toLong()
        val count = raf.readUInt16BE(base + 2) ?: return null
        val stringOffset = raf.readUInt16BE(base + 4) ?: return null
        val storageBase = tableOffset + stringOffset
        val records = mutableListOf<NameRecord>()
        var pos = tableOffset + 6
        for (i in 0 until count) {
            if (pos + 12 > tableOffset + tableLength) break
            val recordBase = pos.toLong()
            val platformId = raf.readUInt16BE(recordBase) ?: break
            val encodingId = raf.readUInt16BE(recordBase + 2) ?: break
            val languageId = raf.readUInt16BE(recordBase + 4) ?: break
            val nameId = raf.readUInt16BE(recordBase + 6) ?: break
            val length = raf.readUInt16BE(recordBase + 8) ?: break
            val offset = raf.readUInt16BE(recordBase + 10) ?: break
            pos += 12
            if (nameId != NAME_ID_TYPOGRAPHIC_FAMILY && nameId != NAME_ID_FONT_FAMILY) continue
            if (length == 0) continue
            val bytes =
                    raf.readBytesBE(storageBase + offset, length) ?: continue
            val text = decodeNameString(platformId, encodingId, bytes) ?: continue
            records.add(NameRecord(nameId, platformId, languageId, text.trim()))
        }
        return pickBestName(records)
    }

    private fun pickBestName(records: List<NameRecord>): String? {
        if (records.isEmpty()) return null
        return records
                .sortedWith(
                        compareByDescending<NameRecord> { it.nameId == NAME_ID_TYPOGRAPHIC_FAMILY }
                                .thenByDescending {
                                    it.platformId == PLATFORM_WINDOWS &&
                                            it.languageId == LANG_ENGLISH_US
                                }
                                .thenByDescending { it.platformId == PLATFORM_WINDOWS }
                                .thenByDescending {
                                    it.platformId == PLATFORM_MAC && it.languageId == 0
                                }
                                .thenByDescending { it.platformId == PLATFORM_MAC }
                                .thenBy { it.text.length }
                )
                .firstOrNull { it.text.isNotEmpty() }
                ?.text
    }

    private fun decodeNameString(platformId: Int, encodingId: Int, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return when (platformId) {
            PLATFORM_WINDOWS ->
                    when (encodingId) {
                        1, 10 -> decodeUtf16Be(bytes)
                        0 -> decodeMacRoman(bytes)
                        else -> decodeUtf16Be(bytes)
                    }
            PLATFORM_MAC ->
                    if (encodingId == 0) decodeMacRoman(bytes) else decodeUtf16Be(bytes)
            0 -> decodeUtf16Be(bytes)
            else ->
                    decodeUtf16Be(bytes)
                            ?: String(bytes, Charsets.US_ASCII).trim { it <= ' ' || it == '\u0000' }
                                    .takeIf { it.isNotEmpty() }
        }
    }

    private fun decodeUtf16Be(bytes: ByteArray): String? {
        if (bytes.size < 2 || bytes.size % 2 != 0) return null
        val chars = CharArray(bytes.size / 2)
        for (i in chars.indices) {
            val hi = bytes[i * 2].toInt() and 0xFF
            val lo = bytes[i * 2 + 1].toInt() and 0xFF
            chars[i] = ((hi shl 8) or lo).toChar()
        }
        return String(chars).trim { it <= ' ' || it == '\u0000' }.takeIf { it.isNotEmpty() }
    }

    private fun decodeMacRoman(bytes: ByteArray): String {
        val charset = Charset.forName("US-ASCII")
        return String(bytes, charset).trim { it <= ' ' || it == '\u0000' }
    }

    private data class NameRecord(
            val nameId: Int,
            val platformId: Int,
            val languageId: Int,
            val text: String,
    )

    private fun RandomAccessFile.readUInt16BE(offset: Long): Int? {
        if (offset < 0 || offset + 1 >= length()) return null
        seek(offset)
        val hi = read()
        val lo = read()
        if (hi < 0 || lo < 0) return null
        return (hi shl 8) or lo
    }

    private fun RandomAccessFile.readUInt32BE(offset: Long): Long? {
        if (offset < 0 || offset + 3 >= length()) return null
        seek(offset)
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) return null
        return ((b0.toLong() shl 24) or (b1.toLong() shl 16) or (b2.toLong() shl 8) or b3.toLong()) and
                0xFFFFFFFFL
    }

    private fun RandomAccessFile.readIntBE(offset: Long): Int? {
        val value = readUInt32BE(offset) ?: return null
        return value.toInt()
    }

    private fun RandomAccessFile.readBytesBE(offset: Int, length: Int): ByteArray? {
        if (length <= 0 || offset < 0 || offset.toLong() + length > this.length()) return null
        seek(offset.toLong())
        val buffer = ByteArray(length)
        var readTotal = 0
        while (readTotal < length) {
            val n = read(buffer, readTotal, length - readTotal)
            if (n <= 0) return null
            readTotal += n
        }
        return buffer
    }
}
