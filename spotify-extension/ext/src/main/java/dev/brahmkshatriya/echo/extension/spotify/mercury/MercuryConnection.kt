package dev.brahmkshatriya.echo.extension.spotify.mercury

import com.google.protobuf.ByteString
import com.spotify.Authentication
import com.spotify.Authentication.APWelcome
import com.spotify.Authentication.ClientResponseEncrypted
import com.spotify.Authentication.LoginCredentials
import com.spotify.Keyexchange
import com.spotify.Keyexchange.APResponseMessage
import com.spotify.Keyexchange.ClientResponsePlaintext
import com.spotify.Keyexchange.ProductFlags
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.extension.spotify.mercury.DiffieHellman.Companion.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class MercuryConnection private constructor() {
    private val keys = DiffieHellman()

    private lateinit var socket: Socket
    private lateinit var dataIn: DataInputStream
    private lateinit var dataOut: DataOutputStream
    private lateinit var cipherPair: CipherPair
    private suspend fun close() = mutex.withLock { socket.close() }

    private lateinit var apWelcome: APWelcome

    private suspend fun getWelcome(credentials: LoginCredentials): APWelcome {
        val clientResponseEncrypted = ClientResponseEncrypted.newBuilder()
            .setLoginCredentials(credentials)
            .setSystemInfo(
                Authentication.SystemInfo.newBuilder()
                    .setOs(Authentication.Os.OS_UNKNOWN)
                    .setCpuFamily(Authentication.CpuFamily.CPU_UNKNOWN)
                    .build()
            )
            .build()
        send(Packet.Type.Login, clientResponseEncrypted.toByteArray())
        val packet = cipherPair.receiveEncoded(dataIn)
        return when (packet.type) {
            Packet.Type.APWelcome -> APWelcome.parseFrom(packet.payload)
            Packet.Type.AuthFailure ->
                throw Exception(Keyexchange.APLoginFailed.parseFrom(packet.payload).toString())

            else -> throw IllegalStateException("Unknown CMD 0x" + Integer.toHexString(packet.cmd.toInt()))
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getStoredToken(
        accessToken: String,
    ): StoredToken {
        open()
        val credentials = LoginCredentials.newBuilder()
            .setTyp(Authentication.AuthenticationType.AUTHENTICATION_SPOTIFY_TOKEN)
            .setAuthData(ByteString.copyFromUtf8(accessToken))
            .build()
        val welcome = getWelcome(credentials)
        close()
        return StoredToken(
            welcome.getCanonicalUsername(),
            Base64.encode(welcome.reusableAuthCredentials.toByteArray())
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun authenticate(
        stored: StoredToken,
    ) {
        open()
        val credentials = LoginCredentials.newBuilder()
            .setUsername(stored.username)
            .setTyp(Authentication.AuthenticationType.AUTHENTICATION_STORED_SPOTIFY_CREDENTIALS)
            .setAuthData(ByteString.copyFrom(Base64.decode(stored.token)))
            .build()
        apWelcome = getWelcome(credentials)
    }

    private suspend fun send(cmd: Packet.Type, payload: ByteArray) {
        cipherPair.sendEncoded(dataOut, cmd.byte, payload)
    }

    suspend fun getKey(gid: String, fileId: String): ByteArray {
        val payload =
            AudioKeyManager.getPayload(ByteString.fromHex(gid), ByteString.fromHex(fileId))
        send(Packet.Type.RequestKey, payload)
        var keyPacket: Packet? = null
        while (keyPacket == null) {
            val packet = cipherPair.receiveEncoded(dataIn)
            val cmd = Packet.Type.parse(packet.cmd)
            when (cmd) {
                Packet.Type.Ping -> runCatching {
                    send(Packet.Type.Pong, packet.payload)
                }.getOrElse {
                    println("Failed sending Pong!" + it.message)
                }

                Packet.Type.AesKey, Packet.Type.AesKeyError -> keyPacket = packet
                else -> Unit
            }
        }
        close()
        return AudioKeyManager.parsePacket(keyPacket)
    }

    private val serverKey = BigInteger(
        "ace0460bffc230aff46bfec3bfbf863da191c6cc336c93a14fb3b01612acac6af180e7f614d9429dbe2e346643e362d2327a1a0d923baedd1402b18155056104d52c96a44c1ecc024ad4b20c001f17edc22fc43521c8f0cbaed2add72b0f9db3c5321a2afe59f35a0dac68f1fa621efb2c8d0cb7392d9247e3d7351a6dbd24c2ae255b88ffab73298a0bcccd0c58673189e8bd3480784a5fc96b899d956bfc86d74f33a6781796c9c32d0d32a5abcd0527e2f710a39613c42f99c027bfed049c3c275804b6b219f9c12f02e94863eca1b642a09d4825f8b39dd0e86af9484da1c2ba863042ea9db3086c190e48b39d66eb0006a25aeea11b13873cd719e655bd",
        16
    ).toByteArray()

    private class Accumulator : DataOutputStream(ByteArrayOutputStream()) {
        private lateinit var bytes: ByteArray
        fun dump() {
            bytes = (out as ByteArrayOutputStream).toByteArray()
            this.close()
        }

        fun array() = bytes
    }

    private val client = OkHttpClient()
    private val request = Request.Builder().url("https://apresolve.spotify.com/").build()
    private val mutex = Mutex()
    private suspend fun open() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val address = client.newCall(request).await().use { response ->
                response.body.string()
                    .substringAfter("[").substringBefore("]").split(",")
                    .map { it.trim().removeSurrounding("\"") }.random()
            }
            socket = address.let { s ->
                val split = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                Socket(split[0], split[1].toInt())
            }
            dataIn = DataInputStream(socket.getInputStream())
            dataOut = DataOutputStream(socket.getOutputStream())
            val acc = Accumulator()

            val nonce = ByteArray(0x10)
            Random.nextBytes(nonce)

            val clientHello = Keyexchange.ClientHello.newBuilder()
                .setBuildInfo(
                    Keyexchange.BuildInfo.newBuilder()
                        .setProduct(Keyexchange.Product.PRODUCT_CLIENT)
                        .addProductFlags(ProductFlags.PRODUCT_FLAG_NONE)
                        .setPlatform(Keyexchange.Platform.PLATFORM_WIN32_X86)
                        .setVersion(117300517)
                        .build()
                )
                .addCryptosuitesSupported(Keyexchange.Cryptosuite.CRYPTO_SUITE_SHANNON)
                .setLoginCryptoHello(
                    Keyexchange.LoginCryptoHelloUnion.newBuilder()
                        .setDiffieHellman(
                            Keyexchange.LoginCryptoDiffieHellmanHello.newBuilder()
                                .setGc(ByteString.copyFrom(keys.publicKeyArray()))
                                .setServerKeysKnown(1)
                                .build()
                        )
                        .build()
                )
                .setClientNonce(ByteString.copyFrom(nonce))
                .setPadding(ByteString.copyFrom(byteArrayOf(0x1e)))
                .build()

            val clientHelloBytes = clientHello.toByteArray()
            var length = 2 + 4 + clientHelloBytes.size
            dataOut.writeByte(0)
            dataOut.writeByte(4)
            dataOut.writeInt(length)
            dataOut.write(clientHelloBytes)
            dataOut.flush()

            acc.writeByte(0)
            acc.writeByte(4)
            acc.writeInt(length)
            acc.write(clientHelloBytes)


            // Read APResponseMessage
            length = dataIn.readInt()
            acc.writeInt(length)
            val buffer = ByteArray(length - 4)
            dataIn.readFully(buffer)
            acc.write(buffer)
            acc.dump()

            val apResponseMessage = APResponseMessage.parseFrom(buffer)
            val sharedKey: ByteArray =
                toByteArray(keys.computeSharedKey(apResponseMessage.challenge.loginCryptoChallenge.diffieHellman.gs.toByteArray()))

            // Check gs_signature
            val factory = KeyFactory.getInstance("RSA")
            val publicKey = factory.generatePublic(
                RSAPublicKeySpec(
                    BigInteger(1, serverKey), BigInteger.valueOf(65537)
                )
            )

            val sig = Signature.getInstance("SHA1withRSA")
            sig.initVerify(publicKey)
            sig.update(apResponseMessage.challenge.loginCryptoChallenge.diffieHellman.gs.toByteArray())
            if (!sig.verify(apResponseMessage.challenge.loginCryptoChallenge.diffieHellman.gsSignature.toByteArray()))
                throw GeneralSecurityException("Failed signature check!")


            // Solve challenge
            val data = ByteArrayOutputStream(0x64)

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(sharedKey, "HmacSHA1"))
            for (i in 1..5) {
                mac.update(acc.array())
                mac.update(byteArrayOf(i.toByte()))
                data.write(mac.doFinal())
                mac.reset()
            }

            val dataArray = data.toByteArray()
            mac.init(SecretKeySpec(Arrays.copyOfRange(dataArray, 0, 0x14), "HmacSHA1"))
            mac.update(acc.array())

            val challenge = mac.doFinal()
            val clientResponsePlaintext = ClientResponsePlaintext.newBuilder()
                .setLoginCryptoResponse(
                    Keyexchange.LoginCryptoResponseUnion.newBuilder()
                        .setDiffieHellman(
                            Keyexchange.LoginCryptoDiffieHellmanResponse.newBuilder()
                                .setHmac(ByteString.copyFrom(challenge)).build()
                        )
                        .build()
                )
                .setPowResponse(Keyexchange.PoWResponseUnion.newBuilder().build())
                .setCryptoResponse(Keyexchange.CryptoResponseUnion.newBuilder().build())
                .build()

            val clientResponsePlaintextBytes = clientResponsePlaintext.toByteArray()
            length = 4 + clientResponsePlaintextBytes.size
            dataOut.writeInt(length)
            dataOut.write(clientResponsePlaintextBytes)
            dataOut.flush()


            try {
                val scrap = ByteArray(4)
                socket.soTimeout = 300
                val read = dataIn.read(scrap)
                if (read == scrap.size) {
                    length =
                        (scrap[0].toInt() shl 24) or (scrap[1].toInt() shl 16) or (scrap[2].toInt() shl 8) or (scrap[3].toInt() and 0xFF)
                    val payload = ByteArray(length - 4)
                    dataIn.readFully(payload)
                    val failed = APResponseMessage.parseFrom(payload).loginFailed
                    throw Exception(failed.toString())
                } else check(read <= 0) { "Read unknown data!" }
            } catch (_: SocketTimeoutException) {
            } finally {
                socket.soTimeout = 0
            }

            cipherPair = CipherPair(
                data.toByteArray().copyOfRange(0x14, 0x34),
                data.toByteArray().copyOfRange(0x34, 0x54)
            )
        }
    }

    companion object {
        suspend fun getAudioKey(stored: StoredToken, gid: String, fileId: String): ByteArray {
            return withTimeout(10000) {
                MercuryConnection().run {
                    authenticate(stored)
                    getKey(gid, fileId)
                }
            }
        }

        suspend fun getStoredToken(accessToken: String): StoredToken {
            return MercuryConnection().getStoredToken(accessToken)
        }
    }
}
