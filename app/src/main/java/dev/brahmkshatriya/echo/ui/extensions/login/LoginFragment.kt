package dev.brahmkshatriya.echo.ui.extensions.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_URI
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
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LoginClient.InputField.Type
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoginFragment : Fragment() {
    companion object {
        fun getBundle(extId: String, extName: String, extensionType: ExtensionType) =
            Bundle().apply {
                putString("extId", extId)
                putString("extName", extName)
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
    private val extId by lazy { requireArguments().getString("extId")!! }
    private val extName by lazy { requireArguments().getString("extName")!! }
    private val loginViewModel by viewModel<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

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
        binding.toolbar.title = getString(R.string.x_login, extName)

        val extension = loginViewModel.extensionLoader.extensions.getFlow(clientType)
            .value?.find { it.id == extId }

        if (extension == null) {
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
        }

        lifecycleScope.launch {
            if (!extension.isClient<LoginClient>()) {
                createSnack(loginNotSupported())
                parentFragmentManager.popBackStack()
                return@launch
            }

            metadata.icon.loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_48dp) {
                binding.extensionIcon.setImageDrawable(it)
            }

            binding.loginContainer.isVisible = true
            val client = extension.instance.value().getOrNull()!!
            val clients = listOfNotNull(
                if (client is LoginClient.WebView) {
                    val button = ButtonExtensionBinding.inflate(
                        layoutInflater, binding.loginToggleGroup, false
                    ).root
                    button.text = getString(R.string.webview)
                    button.setIconResource(R.drawable.ic_language)
                    button to {
                        binding.loginContainer.isVisible = false
                        binding.configureWebView(extension, client)
                    }
                } else null,
                *(if (client is LoginClient.CustomInput) {
                    client.forms.map {
                        val button = ButtonExtensionBinding.inflate(
                            layoutInflater, binding.loginToggleGroup, false
                        ).root
                        button.text = it.label
                        button.setIconResource(getIcon(it.icon))
                        button to {
                            binding.loginContainer.isVisible = false
                            binding.configureCustomTextInput(extension, it)
                        }
                    }
                } else listOf()).toTypedArray(),
            )
            if (clients.size == 1) clients.first().second() else {
                binding.loginToggleGroup.isVisible = true
                clients.forEachIndexed { index, pair ->
                    val button = pair.first
                    button.setOnClickListener { pair.second() }
                    binding.loginToggleGroup.addView(button)
                    button.id = index
                }
            }
        }
    }

    private fun loginNotSupported(): String {
        val login = getString(R.string.login)
        return getString(R.string.x_is_not_supported_in_x, login, extName)
    }

    private fun FragmentLoginBinding.configureWebView(
        extension: Extension<*>,
        client: LoginClient.WebView
    ) = with(client) {
        val mutex = Mutex()
        val data = mutableMapOf<String, String>()

        webView.isVisible = true
        webView.applyDarkMode()

        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView.goBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        webView.webViewClient = object : WebViewClient() {

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = webView.canGoBack()
            }

            fun checkForCookies(url: String) {
                if (runCatching { loginWebViewCookieUrlRegex?.find(url) }.getOrNull() != null) {
                    lifecycleScope.launch {
                        mutex.withLock {
                            val result = webView.loadData(url, client)
                            data[url] = result
                        }
                    }
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                checkForCookies(request?.url.toString())
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                if (runCatching { loginWebViewStopUrlRegex.find(url) }.getOrNull() != null) {
                    webView.stopLoading()
                    lifecycleScope.launch {
                        mutex.withLock {
                            val result = webView.loadData(url, client)
                            webView.isVisible = false
                            loadingContainer.root.isVisible = true
                            callback.isEnabled = false

                            WebStorage.getInstance().deleteAllData()
                            CookieManager.getInstance().run {
                                removeAllCookies(null)
                                flush()
                            }

                            data[url] = result
                            loginViewModel.onWebViewStop(extension, url, data)
                        }
                    }
                    return true
                }
                checkForCookies(url)
                return false
            }
        }

        webView.settings.apply {
            domStorageEnabled = true
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            userAgentString = loginWebViewInitialUrl.headers["User-Agent"] ?: USER_AGENT
        }

        webView.loadUrl(loginWebViewInitialUrl.url, loginWebViewInitialUrl.headers)

        lifecycleScope.launch {
            delay(1000)
            appBarLayout.setExpanded(false, true)
        }
        Unit
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
        form: LoginClient.Form
    ) {
        customInputContainer.isVisible = true
        form.inputFields.forEachIndexed { index, field ->
            val input = ItemInputBinding.inflate(
                layoutInflater, customInput, false
            )
            input.root.hint = field.label
            input.root.setStartIconDrawable(getIcon(field.type))
            input.editText.inputType = when (field.type) {
                Type.Email -> TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                Type.Password -> TYPE_TEXT_VARIATION_PASSWORD
                Type.Number -> TYPE_CLASS_NUMBER
                Type.Url -> TYPE_TEXT_VARIATION_URI
                else -> TYPE_CLASS_TEXT
            }
            input.editText.setText(loginViewModel.inputs[field.key])
            input.editText.doAfterTextChanged { editable ->
                loginViewModel.inputs[field.key] = editable.toString().takeIf { it.isNotBlank() }
            }
            input.editText.setOnEditorActionListener { _, _, _ ->
                if (index < form.inputFields.size - 1) {
                    customInput.getChildAt(index + 1).requestFocus()
                } else loginCustomSubmit.performClick()
                true
            }
            customInput.addView(input.root)
        }
        loginCustomSubmit.setOnClickListener {
            form.inputFields.forEach {
                if (it.isRequired && loginViewModel.inputs[it.key].isNullOrEmpty()) {
                    message(Message(getString(R.string.x_is_required, it.label)))
                    return@setOnClickListener
                }
                val regex = it.regex
                if (regex != null && !loginViewModel.inputs[it.key].isNullOrEmpty()) {
                    if (!loginViewModel.inputs[it.key]!!.matches(regex)) {
                        message(
                            Message(
                                getString(R.string.regex_invalid, it.label, regex.pattern)
                            )
                        )
                        return@setOnClickListener
                    }
                }
            }
            loginViewModel.onCustomTextInputSubmit(extension, form)
            customInputContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }

    private fun message(m: Message) {
        lifecycleScope.launch {
            loginViewModel.messageFlow.emit(m)
        }
    }

    private fun getIcon(type: Type) = when (type) {
        Type.Email -> R.drawable.ic_email
        Type.Password -> R.drawable.ic_password
        Type.Number -> R.drawable.ic_numbers
        Type.Url -> R.drawable.ic_language
        Type.Username -> R.drawable.ic_account_circle
        Type.Misc -> R.drawable.ic_input
    }
}



