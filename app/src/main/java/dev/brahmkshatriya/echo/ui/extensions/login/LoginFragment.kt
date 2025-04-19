package dev.brahmkshatriya.echo.ui.extensions.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.databinding.ItemInputBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.exceptions.AppException
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoginFragment : Fragment() {
    companion object {
        fun getBundle(clientId: String, clientName: String, extensionType: ExtensionType) =
            Bundle().apply {
                putString("clientId", clientId)
                putString("clientName", clientName)
                putString("extensionType", extensionType.name)
            }

        fun getBundle(error: AppException.LoginRequired) =
            getBundle(error.extension.id, error.extension.name, error.extension.type)

        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
    }

    private var binding by autoCleared<FragmentLoginBinding>()
    private val clientType by lazy {
        val type = requireArguments().getString("extensionType")!!
        ExtensionType.valueOf(type)
    }
    private val clientId by lazy { requireArguments().getString("clientId")!! }
    private val clientName by lazy { requireArguments().getString("clientName")!! }
    private val loginViewModel by viewModel<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private suspend inline fun <reified T : LoginClient> Extension<*>.getClient(
        button: MaterialButton, noinline configure: FragmentLoginBinding.(T) -> Unit
    ) = run {
        val client = instance.value().getOrNull()
        if (client !is T) null
        else Pair(button) { configure(binding, client) }
    }

    var clients = listOf<Pair<MaterialButton, () -> Unit>>()
    var current: Int? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.loginContainer.applyContentInsets(it)
            binding.loadingContainer.root.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
            binding.iconContainer.alpha = 1 - offset
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.title = getString(R.string.x_login, clientName)

        val extension = loginViewModel.extensionLoader.extensions.getFlow(clientType)
            .value?.find { it.id == clientId }

        if (extension == null) {
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata

        observe(loginViewModel.loginClient) {
            it ?: return@observe
            if (current == it) return@observe
            current = it
            binding.loginToggleGroup.isVisible = false
            clients[it].second()
        }

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
        }

        lifecycleScope.launch {
            if (!extension.isClient<LoginClient>()) {
                createSnack(loginNotSupported())
                parentFragmentManager.popBackStack()
                return@launch
            }

            metadata.icon
                .loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_48dp) {
                    binding.extensionIcon.setImageDrawable(it)
                }

            binding.loginContainer.isVisible = true

            val clients = listOfNotNull(
                extension.getClient<LoginClient.UsernamePassword>(binding.loginUserPass) {
                    configureUsernamePassword(extension)
                },
                extension.getClient<LoginClient.WebView>(binding.loginWebview) {
                    configureWebView(extension, it)
                },
                extension.getClient<LoginClient.CustomTextInput>(binding.loginCustomInput) {
                    configureCustomTextInput(extension, it)
                },
            )
            this@LoginFragment.clients = clients
            if (clients.isEmpty()) {
                createSnack(loginNotSupported())
                parentFragmentManager.popBackStack()
                return@launch
            } else if (clients.size == 1) loginViewModel.loginClient.value = 0
            else {
                binding.loginToggleGroup.isVisible = true
                clients.forEachIndexed { index, pair ->
                    val (button, _) = pair
                    button.isVisible = true
                    button.setOnClickListener {
                        loginViewModel.loginClient.value = index
                        binding.loginToggleGroup.isVisible = false
                    }
                }
            }
        }
    }

    private fun loginNotSupported(): String {
        val login = getString(R.string.login)
        return getString(R.string.x_is_not_supported_in_x, login, clientName)
    }

    private fun FragmentLoginBinding.configureWebView(
        extension: Extension<*>,
        client: LoginClient.WebView
    ) = with(client) {
        webView.isVisible = true
        webView.applyDarkMode()
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView.goBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        /*ServiceWorkerController.getInstance()
            .setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()
                    println("FUCK YOU SW $url")
                    val cookie = lifecycleScope.launch { webView.loadData(url, client) }
                    println("FUCK YOU $cookie")
                    if (runCatching { loginWebViewStopUrlRegex.find(url) }.getOrNull() != null) {
                        Handler(Looper.getMainLooper()).post {
                            lifecycleScope.launch {
                                webView.stopLoading()
                                val data = webView.loadData(url, client)
                                webView.isVisible = false
                                loadingContainer.root.isVisible = true
                                WebStorage.getInstance().deleteAllData()
                                CookieManager.getInstance().apply {
                                    removeAllCookies(null)
                                    flush()
                                }
                                loginViewModel.onWebViewStop(extension, url, data)
                            }
                        }
                    }
                    return null
                }
            })*/

        webView.webViewClient = object : WebViewClient() {
            /*override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                println("FUCK YOU $url")
                callback.isEnabled = webView.canGoBack()
                url ?: return
                if (runCatching { loginWebViewStopUrlRegex.find(url) }.getOrNull() != null) {
                    webView.stopLoading()
                    lifecycleScope.launch {
                        val data = webView.loadData(url, client)
                        webView.isVisible = false
                        loadingContainer.root.isVisible = true
                        callback.isEnabled = false
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().run {
                            removeAllCookies(null)
                            flush()
                        }
                        loginViewModel.onWebViewStop(extension, url, data)
                    }
                }
            }*/

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val data = mutableListOf<String>()
                val url = request?.url.toString()
                if(runCatching { loginWebViewCookieUrlRegex.find(url) }.getOrNull() != null) {
                    lifecycleScope.launch {
                        data.add(webView.loadData(url, client))
                    }
                }
                println("FUCK YOU $url")
                if (runCatching { loginWebViewStopUrlRegex.find(url) }.getOrNull() != null) {
                    webView.stopLoading()
                    lifecycleScope.launch {
                        data.add(webView.loadData(url, client))
                        webView.isVisible = false
                        loadingContainer.root.isVisible = true
                        callback.isEnabled = false
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().run {
                            removeAllCookies(null)
                            flush()
                        }
                        loginViewModel.onWebViewStop(extension, url, data)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.settings.apply {
            domStorageEnabled = true
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            userAgentString = loginWebViewInitialUrl.headers["User-Agent"]
                ?: USER_AGENT
        }

        webView.loadUrl(loginWebViewInitialUrl.url, loginWebViewInitialUrl.headers)

        lifecycleScope.launch {
            delay(1000)
            appBarLayout.setExpanded(false, true)
        }
    }


    private suspend fun WebView.loadData(url: String, client: LoginClient.WebView) = when (client) {
        is LoginClient.WebView.Cookie ->
            CookieManager.getInstance().getCookie(url) ?: ""

        is LoginClient.WebView.Evaluate -> suspendCoroutine {
            evaluateJavascript(client.javascriptToEvaluate) { result -> it.resume(result) }
        }
    }

    private fun WebView.applyDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.isAlgorithmicDarkeningAllowed = true
        }
    }

    private fun FragmentLoginBinding.configureCustomTextInput(
        extension: Extension<*>,
        client: LoginClient.CustomTextInput
    ) {
        customInputContainer.isVisible = true
        client.loginInputFields.forEachIndexed { index, field ->
            val input = ItemInputBinding.inflate(
                layoutInflater,
                customInput,
                false
            )
            input.root.hint = field.label
            input.editText.inputType = if (!field.isPassword) TYPE_CLASS_TEXT
            else TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
            input.editText.setText(loginViewModel.inputs[field.key])
            input.editText.doAfterTextChanged { editable ->
                loginViewModel.inputs[field.key] = editable.toString().takeIf { it.isNotBlank() }
            }
            input.editText.setOnEditorActionListener { _, _, _ ->
                if (index < client.loginInputFields.size - 1) {
                    customInput.getChildAt(index + 1).requestFocus()
                } else loginCustomSubmit.performClick()
                true
            }
            customInput.addView(input.root)
        }
        loginCustomSubmit.setOnClickListener {
            client.loginInputFields.forEach {
                if (it.isRequired && loginViewModel.inputs[it.key].isNullOrEmpty()) {
                    lifecycleScope.launch {
                        loginViewModel.messageFlow.emit(
                            Message(getString(R.string.x_is_required, it.label))
                        )
                    }
                    return@setOnClickListener
                }
            }
            loginViewModel.onCustomTextInputSubmit(extension)
            customInputContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }

    private fun FragmentLoginBinding.configureUsernamePassword(
        extension: Extension<*>
    ) {
        usernamePasswordContainer.isVisible = true
        loginUsername.requestFocus()
        loginUsername.setOnEditorActionListener { _, _, _ ->
            loginPassword.requestFocus()
            true
        }
        loginPassword.setOnEditorActionListener { _, _, _ ->
            loginUserPassSubmit.performClick()
            true
        }
        loginUserPassSubmit.setOnClickListener {
            val username = loginUsername.text.toString()
            val password = loginPassword.text.toString()
            if (username.isEmpty()) {
                lifecycleScope.launch {
                    loginViewModel.messageFlow.emit(
                        Message(
                            getString(R.string.x_is_required, getString(R.string.username))
                        )
                    )
                }
                return@setOnClickListener
            }
            loginViewModel.onUsernamePasswordSubmit(extension, username, password)
            usernamePasswordContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }
}



