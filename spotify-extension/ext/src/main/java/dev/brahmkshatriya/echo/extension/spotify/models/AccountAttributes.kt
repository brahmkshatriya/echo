package dev.brahmkshatriya.echo.extension.spotify.models

import kotlinx.serialization.Serializable

@Serializable
data class AccountAttributes (
    val data: Data
) {

    @Serializable
    data class Data(
        val me: Me
    )

    @Serializable
    data class Me(
        val account: Account
    )

    @Serializable
    data class Account(
        val country: String? = null,
        val product: Product
    )

    @Suppress("unused")
    @Serializable
    enum class Product {
        FREE, PREMIUM, FAMILY, STUDENT
    }
}