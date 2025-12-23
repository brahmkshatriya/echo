package dev.brahmkshatriya.echo.extension.spotify.mercury

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicInteger

class CipherPair(sendKey: ByteArray, receiveKey: ByteArray) {
    private val sendCipher = Shannon()
    private val receiveCipher = Shannon()
    private val sendNonce = AtomicInteger(0)
    private val receiveNonce = AtomicInteger(0)
    private val sendMutex = Mutex()
    private val receiveMutex = Mutex()

    init {
        sendCipher.key(sendKey)
        receiveCipher.key(receiveKey)
    }

    suspend fun sendEncoded(out: OutputStream, cmd: Byte, payload: ByteArray) = sendMutex.withLock {
        withContext(Dispatchers.IO) {
            sendCipher.nonce(toByteArray(sendNonce.getAndIncrement()))
            val buffer = ByteBuffer.allocate(1 + 2 + payload.size)
            buffer.put(cmd)
                .putShort(payload.size.toShort())
                .put(payload)

            val bytes = buffer.array()
            sendCipher.encrypt(bytes)

            val mac = ByteArray(4)
            sendCipher.finish(mac)

            out.write(bytes)
            out.write(mac)
            out.flush()
        }
    }

    suspend fun receiveEncoded(`in`: DataInputStream) = receiveMutex.withLock {
        withContext(Dispatchers.IO) {
            receiveCipher.nonce(toByteArray(receiveNonce.getAndIncrement()))
            val headerBytes = ByteArray(3)
            `in`.readFully(headerBytes)
            receiveCipher.decrypt(headerBytes)

            val cmd = headerBytes[0]
            val payloadLength =
                ((headerBytes[1].toInt() shl 8) or (headerBytes[2].toInt() and 0xFF)).toShort()

            val payloadBytes = ByteArray(payloadLength.toInt())
            `in`.readFully(payloadBytes)
            receiveCipher.decrypt(payloadBytes)

            val mac = ByteArray(4)
            `in`.readFully(mac)

            val expectedMac = ByteArray(4)
            receiveCipher.finish(expectedMac)
            if (!mac.contentEquals(expectedMac)) throw GeneralSecurityException("MACs don't match!")
            Packet(cmd, payloadBytes)
        }
    }

    companion object {
        fun toByteArray(i: Int): ByteArray {
            val buffer = ByteBuffer.allocate(4)
            buffer.putInt(i)
            return buffer.array()
        }
    }
}