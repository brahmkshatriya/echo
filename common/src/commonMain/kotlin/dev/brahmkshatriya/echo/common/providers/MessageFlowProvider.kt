package dev.brahmkshatriya.echo.common.providers

import dev.brahmkshatriya.echo.common.models.Message
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Interface to provide functionality of sending messages to the app, via using a shared flow.
 *
 * @see MutableSharedFlow
 * @see Message
 */
interface MessageFlowProvider {
    /**
     * Injecting the message flow, called once when the extension is initialized.
     *
     * use the following to use the message flow:
     * ```kotlin
     * messageFlow.emit(Message("Hello World!"))
     * // or
     * val action = Action("Click me!") { /* do something */  }
     * messageFlow.emit(Message("Hello World!", action))
     * ```
     *
     * @param messageFlow The shared flow to send messages to the app.
     */
    fun setMessageFlow(messageFlow: MutableSharedFlow<Message>)
}