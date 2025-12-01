package dev.brahmkshatriya.echo.extension.spotify.mercury

import com.google.protobuf.ByteString
import dev.brahmkshatriya.echo.extension.spotify.mercury.CipherPair.Companion.toByteArray
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object AudioKeyManager {

    fun getPayload(
        gid: ByteString, fileId: ByteString
    ): ByteArray {
        val seq = 0
        val out = ByteArrayOutputStream()
        fileId.writeTo(out)
        gid.writeTo(out)
        out.write(toByteArray(seq))
        out.write(ZERO_SHORT)
        return out.toByteArray()
    }

    fun parsePacket(packet: Packet): ByteArray {
        val payload = ByteBuffer.wrap(packet.payload)
        payload.getInt()
        return when (packet.type) {
            Packet.Type.AesKey -> {
                val key = ByteArray(16)
                payload[key]
                key
            }

            Packet.Type.AesKeyError -> {
                val code = payload.getShort().toInt()
                throw AesKeyError(code)
            }

            else -> throw Exception("Couldn't handle packet, cmd: ${packet.type}, length: ${packet.payload.size}")
        }
    }

    class AesKeyError(val code: Int) : Exception("Error fetching audio key, code: $code")

    private val ZERO_SHORT = byteArrayOf(0, 0)
}