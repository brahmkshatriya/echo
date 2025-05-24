package dev.brahmkshatriya.echo.common.clients

import dev.brahmkshatriya.echo.common.clients.LoginClient.CustomInput
import dev.brahmkshatriya.echo.common.clients.LoginClient.WebView
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.User

/**
 * To be implemented by the extension to provide login functionality.
 *
 * Do not implement this interface directly, use the sub-interfaces.
 * The extension can implement all of the sub-interfaces.
 *
 * @see [WebView]
 * @see [CustomInput]
 */
sealed interface LoginClient {


    /**
     * Interface when the login requires a webview.
     *
     * The extension should provide the [webViewRequest]
     *
     * @see [WebViewRequest]
     */
    interface WebView : LoginClient {

        /**
         * The request to be made to the webview, should return a list of users.
         *
         * The [WebViewRequest.maxTimeout] is ignored for login.
         *
         * @see WebViewRequest
         * @see LoginClient.WebView
         */
        val webViewRequest: WebViewRequest<List<User>>
    }

    /**
     * To be implemented when the login screen has custom text input fields.
     *
     * The extension needs to provide the [forms].
     */
    interface CustomInput : LoginClient {

        /**
         * List of forms to be displayed on the login screen.
         *
         * @see Form
         * @see LoginClient.CustomInput
         */
        val forms: List<Form>

        /**
         * Called when the user submits the login form.
         *
         * @param data A map of the input fields with the key as the `key` from the `InputField` and the value as the user input
         *
         * @return A list of users that are logged in
         *
         * @see LoginClient.CustomInput
         */
        suspend fun onLogin(key: String, data: Map<String, String?>): List<User>
    }

    /**
     * Represents a form for the login screen.
     *
     * @param key The key to be used to identify the `data` in the `onLogin` method
     * @param label The label to be displayed for the form
     * @param icon The icon to be displayed for the form
     * @param inputFields The list of input fields to be displayed in the form
     *
     * @see InputField
     * @see LoginClient.CustomInput
     */
    data class Form(
        val key: String,
        val label: String,
        val icon: InputField.Type,
        val inputFields: List<InputField>,
    )

    /**
     * Represents an input field for the login screen.
     *
     * @param type The type of the input field
     * @param key The key to be used in the `data` map in the `onLogin` method
     * @param label The label to be displayed for the input field
     * @param isRequired If the field is required
     * @param regex The regex to be used for validation of the input field
     */
    data class InputField(
        val type: Type,
        val key: String,
        val label: String,
        val isRequired: Boolean,
        val regex: Regex? = null
    ) {
        enum class Type {
            Email, Username, Password, Number, Url, Misc
        }
    }

    /**
     * Called when the extension starts or when user selects a user.
     * `null` if no user is logged in (can also be Incognito mode)
     */
    suspend fun onSetLoginUser(user: User?)

    /**
     * To be called when any other extension needs the current user.
     * Be sure to remove any sensitive data.
     */
    suspend fun getCurrentUser(): User?
}