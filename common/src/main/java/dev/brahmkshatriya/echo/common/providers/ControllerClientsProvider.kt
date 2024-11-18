package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.ControllerExtension

interface ControllerClientsProvider {
    val requiredControllerClients: List<String>
    fun setControllerExtensions(controllerClients: List<ControllerExtension>)
}