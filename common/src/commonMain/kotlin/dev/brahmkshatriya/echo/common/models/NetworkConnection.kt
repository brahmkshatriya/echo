package dev.brahmkshatriya.echo.common.models

import dev.brahmkshatriya.echo.common.models.NetworkConnection.Metered
import dev.brahmkshatriya.echo.common.models.NetworkConnection.NotConnected
import dev.brahmkshatriya.echo.common.models.NetworkConnection.Unmetered


/**
 * Enum class to define the type of network,
 * - [NotConnected]: No network connection
 * - [Metered]: Connected to a metered network (e.g., mobile data)
 * - [Unmetered]: Connected to an unmetered network (e.g., Wi-Fi)
 */
enum class NetworkConnection {
    NotConnected, Metered, Unmetered
}