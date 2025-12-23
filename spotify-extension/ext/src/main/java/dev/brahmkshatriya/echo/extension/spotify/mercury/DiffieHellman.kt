package dev.brahmkshatriya.echo.extension.spotify.mercury

import java.math.BigInteger
import java.util.Arrays
import kotlin.random.Random

class DiffieHellman {
    private val privateKey: BigInteger
    private val publicKey: BigInteger

    init {
        val keyData = ByteArray(95)
        Random.nextBytes(keyData)

        privateKey = BigInteger(1, keyData)
        publicKey = GENERATOR.modPow(privateKey, PRIME)
    }

    fun computeSharedKey(remoteKeyBytes: ByteArray): BigInteger {
        val remoteKey = BigInteger(1, remoteKeyBytes)
        return remoteKey.modPow(privateKey, PRIME)
    }

    fun publicKeyArray() = toByteArray(publicKey)

    companion object {
        fun toByteArray(i: BigInteger): ByteArray {
            var array = i.toByteArray()
            if (array[0].toInt() == 0) array = Arrays.copyOfRange(array, 1, array.size)
            return array
        }

        private val GENERATOR: BigInteger = BigInteger.valueOf(2)
        private val PRIME = BigInteger(
            "ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a63a3620ffffffffffffffff",
            16
        )
    }
}