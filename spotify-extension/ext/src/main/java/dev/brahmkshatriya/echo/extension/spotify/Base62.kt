package dev.brahmkshatriya.echo.extension.spotify

import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.ln

object Base62 {
    private const val STANDARD_BASE = 256
    private const val TARGET_BASE = 62
    private const val DEFAULT_LENGTH = 16

    private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        .map { it.code.toByte() }.toByteArray()
    private val lookup = ByteArray(256) { i -> alphabet.indexOf(i.toByte()).toByte() }

    fun encode(message: String, length: Int = DEFAULT_LENGTH): String {
        val bytes = hexToBytes(message)
        val indices = convert(bytes, STANDARD_BASE, TARGET_BASE, length)
        val encoded = translate(indices, alphabet).decodeToString()
        val zerosToAdd = 22 - encoded.length
        return "0".repeat(maxOf(zerosToAdd, 0)) + encoded
    }

    fun decode(encoded: String, length: Int = DEFAULT_LENGTH): String {
        val prepared = translate(encoded.toByteArray(), lookup)
        val decoded = convert(prepared, TARGET_BASE, STANDARD_BASE, length)
        return bytesToHex(decoded)
    }

    private fun translate(indices: ByteArray, dictionary: ByteArray) =
        ByteArray(indices.size) { dictionary[indices[it].toInt() and 0xFF] }

    private fun convert(input: ByteArray, sourceBase: Int, targetBase: Int, length: Int): ByteArray {
        val estimatedLength = if (length == -1)
            ceil((ln(sourceBase.toDouble()) / ln(targetBase.toDouble())) * input.size).toInt()
        else length
        val output = ByteArrayOutputStream(estimatedLength)
        var source = input
        while (source.any { it != 0.toByte() }) {
            val quotient = ByteArrayOutputStream(source.size)
            var remainder = 0
            for (b in source) {
                val accumulator = (b.toInt() and 0xFF) + remainder * sourceBase
                remainder = accumulator % targetBase
                if (quotient.size() > 0 || accumulator / targetBase > 0) {
                    quotient.write(accumulator / targetBase)
                }
            }
            output.write(remainder)
            source = quotient.toByteArray()
        }
        while (output.size() < estimatedLength) output.write(0)
        return output.toByteArray().reversedArray()
    }

    private val hexArray = "0123456789abcdef".toCharArray()

    private fun bytesToHex(bytes: ByteArray): String = buildString {
        bytes.forEach { byte ->
            append(hexArray[(byte.toInt() ushr 4) and 0x0F])
            append(hexArray[byte.toInt() and 0x0F])
        }
    }

    private fun hexToBytes(str: String) =
        ByteArray(str.length / 2) {
            ((str[it * 2].digitToInt(16) shl 4) + str[it * 2 + 1].digitToInt(16))
                .toByte()
        }
}