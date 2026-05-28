package com.lu4p.fokuslauncher.data.font

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Test

class TtfFontMetadataReaderTest {

    @Test
    fun `readFamilyDisplayName reads typographic family from name table`() {
        val file = File.createTempFile("fokus_font_test", ".ttf")
        try {
            writeMinimalTtfWithName(file, familyName = "Fira Sans")
            assertEquals("Fira Sans", TtfFontMetadataReader.readFamilyDisplayName(file))
        } finally {
            file.delete()
        }
    }

    /** Builds a tiny valid-enough TTF with only offset + name tables for unit tests. */
    private fun writeMinimalTtfWithName(file: File, familyName: String) {
        val nameBytes = familyName.toByteArray(Charsets.UTF_16BE)
        val nameRecordSize = 12
        val nameHeaderSize = 6
        val nameCount = 1
        val stringOffset = nameHeaderSize + nameCount * nameRecordSize
        val nameTableSize = stringOffset + nameBytes.size
        val offsetTableSize = 12 + 16 // one table record
        val fileSize = offsetTableSize + nameTableSize

        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(fileSize.toLong())
            // sfnt version 1.0
            raf.writeIntBE(0x00010000)
            raf.writeUInt16BE(1) // numTables
            raf.writeUInt16BE(16) // searchRange
            raf.writeUInt16BE(0) // entrySelector
            raf.writeUInt16BE(0) // rangeShift
            // name table record
            raf.writeIntBE(0x6E616D65)
            raf.writeIntBE(0)
            raf.writeUInt32BE(offsetTableSize)
            raf.writeUInt32BE(nameTableSize)
            // name table
            val nameBase = offsetTableSize
            raf.writeUInt16BE(nameBase, 0) // format
            raf.writeUInt16BE(nameBase + 2, nameCount)
            raf.writeUInt16BE(nameBase + 4, stringOffset)
            val recordPos = nameBase + nameHeaderSize
            raf.writeUInt16BE(recordPos, 3) // Windows
            raf.writeUInt16BE(recordPos + 2, 1) // Unicode BMP
            raf.writeUInt16BE(recordPos + 4, 0x0409) // en-US
            raf.writeUInt16BE(recordPos + 6, 16) // typographic family
            raf.writeUInt16BE(recordPos + 8, nameBytes.size)
            raf.writeUInt16BE(recordPos + 10, 0)
            raf.writeBytesBE(nameBase + stringOffset, nameBytes)
        }
    }

    private fun RandomAccessFile.writeIntBE(value: Int) {
        write(
                byteArrayOf(
                        (value shr 24).toByte(),
                        (value shr 16).toByte(),
                        (value shr 8).toByte(),
                        value.toByte(),
                )
        )
    }

    private fun RandomAccessFile.writeUInt16BE(offset: Int, value: Int) {
        seek(offset.toLong())
        write(byteArrayOf((value shr 8).toByte(), value.toByte()))
    }

    private fun RandomAccessFile.writeUInt16BE(value: Int) {
        write(byteArrayOf((value shr 8).toByte(), value.toByte()))
    }

    private fun RandomAccessFile.writeUInt32BE(value: Int) {
        write(
                byteArrayOf(
                        (value shr 24).toByte(),
                        (value shr 16).toByte(),
                        (value shr 8).toByte(),
                        value.toByte(),
                )
        )
    }

    private fun RandomAccessFile.writeBytesBE(offset: Int, bytes: ByteArray) {
        seek(offset.toLong())
        write(bytes)
    }
}
