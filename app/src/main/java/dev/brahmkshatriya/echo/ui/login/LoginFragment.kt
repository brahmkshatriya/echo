package dev.brahmkshatriya.echo.ui.login

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.databinding.ItemInputBinding
import dev.brahmkshatriya.echo.plugger.echo.ExtensionInfo
import dev.brahmkshatriya.echo.plugger.echo.getExtension
import dev.brahmkshatriya.echo.ui.exception.AppException
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.collect
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.loginNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class LoginFragment : Fragment() {
    companion object {
        fun newInstance(clientId: String, clientName: String, extensionType: ExtensionType) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString("clientId", clientId)
                    putString("clientName", clientName)
                    putString("extensionType", extensionType.name)
                }
            }

        fun newInstance(error: AppException.LoginRequired) =
            newInstance(error.extensionId, error.extensionName, error.extensionType)

    }

    private var binding by autoCleared<FragmentLoginBinding>()
    private val clientType by lazy {
        val type = requireArguments().getString("extensionType")!!
        ExtensionType.valueOf(type)
    }
    private val clientId by lazy { requireArguments().getString("clientId")!! }
    private val clientName by lazy { requireArguments().getString("clientName")!! }
    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun createUsernamePasswordClient(client: LoginClient) =
        if (client is LoginClient.UsernamePassword) object : LoginClient.UsernamePassword {
            override suspend fun onLogin(username: String, password: String) =
                client.onLogin(username, password)

            override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
            override suspend fun getCurrentUser() = client.getCurrentUser()
        } to binding.loginUserPass
        else null

    private fun createWebViewClient(client: LoginClient) =
        if (client is LoginClient.WebView) when (client) {
            is LoginClient.WebView.Cookie -> object : LoginClient.WebView.Cookie {
                override val loginWebViewInitialUrl = client.loginWebViewInitialUrl
                override val loginWebViewStopUrlRegex = client.loginWebViewStopUrlRegex
                override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
                override suspend fun getCurrentUser() = client.getCurrentUser()
                override suspend fun onLoginWebviewStop(url: String, data: String) =
                    client.onLoginWebviewStop(url, data)
            }

            is LoginClient.WebView.Evaluate -> object : LoginClient.WebView.Evaluate {
                override val loginWebViewInitialUrl = client.loginWebViewInitialUrl
                override val loginWebViewStopUrlRegex = client.loginWebViewStopUrlRegex
                override val javascriptToEvaluate = client.javascriptToEvaluate
                override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
                override suspend fun getCurrentUser() = client.getCurrentUser()
                override suspend fun onLoginWebviewStop(url: String, data: String) =
                    client.onLoginWebviewStop(url, data)
            }

            else -> null
        } to binding.loginWebview
        else null

    private fun createCustomTextInputClient(client: LoginClient) =
        if (client is LoginClient.CustomTextInput) object : LoginClient.CustomTextInput {
            override val loginInputFields = client.loginInputFields
            override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
            override suspend fun getCurrentUser() = client.getCurrentUser()
            override suspend fun onLogin(data: Map<String, String?>) = client.onLogin(data)
        } to binding.loginCustomInput
        else null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.iconContainer.updatePadding(top = it.top)
            binding.loginContainer.applyContentInsets(it)
            binding.loadingContainer.root.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.title = getString(R.string.extension_login, clientName)

        val pair = when (clientType) {
            ExtensionType.MUSIC -> {
                val extension = loginViewModel.extensionList.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }

            ExtensionType.TRACKER -> {
                val extension = loginViewModel.trackerList.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }

            ExtensionType.LYRICS -> {
                val extension = loginViewModel.lyricsList.getExtension(clientId)
                if (extension == null) null else extension.metadata to extension.client
            }
        }

        if (pair == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }
        val (metadata, client) = pair

        if (client as? LoginClient == null) {
            createSnack(requireContext().loginNotSupported(clientName))
            parentFragmentManager.popBackStack()
            return
        }

        metadata.iconUrl?.toImageHolder().loadWith(binding.extensionIcon, R.drawable.ic_extension) {
            binding.extensionIcon.setImageDrawable(it)
        }

        binding.loginContainer.isVisible = true

        val info = ExtensionInfo(metadata, clientType)
        val clients = listOfNotNull(
            createUsernamePasswordClient(client),
            createWebViewClient(client),
            createCustomTextInputClient(client)
        )

        if (clients.isEmpty()) {
            createSnack(requireContext().loginNotSupported(clientName))
            parentFragmentManager.popBackStack()
            return
        } else if (clients.size == 1) loginViewModel.loginClient.value = clients.first().first
        else {
            binding.loginToggleGroup.isVisible = true
            clients.forEach { (loginClient, button) ->
                button.isVisible = true
                button.setOnClickListener {
                    loginViewModel.loginClient.value = loginClient
                    binding.loginToggleGroup.isVisible = false
                }
            }
        }
        collect(loginViewModel.loginClient) { loginClient ->
            loginClient ?: return@collect
            binding.loginToggleGroup.isVisible = false
            when (loginClient) {
                is LoginClient.WebView ->
                    binding.configureWebView(info, loginClient)

                is LoginClient.CustomTextInput ->
                    binding.configureCustomTextInput(info, loginClient)

                is LoginClient.UsernamePassword ->
                    binding.configureUsernamePassword(info, loginClient)
            }
        }

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun FragmentLoginBinding.configureWebView(
        info: ExtensionInfo,
        client: LoginClient.WebView
    ) = with(client) {
        webViewContainer.isVisible = true
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
                url ?: return
                lifecycleScope.launch {
                    if (loginWebViewStopUrlRegex.matches(url)) {
                        webView.stopLoading()
                        val data = webView.loadData(url, client)
                        loginViewModel.onWebViewStop(info, client, url, data)
                        CookieManager.getInstance().run {
                            removeAllCookies(null)
                            flush()
                        }
                        webViewContainer.isVisible = false
                        loadingContainer.root.isVisible = true
                        callback.isEnabled = false
                    }
                }
            }
        }
        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            databaseEnabled = true
            userAgentString = loginWebViewInitialUrl.headers["User-Agent"]
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
        info: ExtensionInfo,
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
                            SnackBar.Message(
                                getString(
                                    R.string.required_field,
                                    it.label
                                )
                            )
                        )
                    }
                    return@setOnClickListener
                }
            }
            loginViewModel.onCustomTextInputSubmit(info, client)
            customInputContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }

    private fun FragmentLoginBinding.configureUsernamePassword(
        info: ExtensionInfo,
        client: LoginClient.UsernamePassword
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
                        SnackBar.Message(
                            getString(R.string.required_field, getString(R.string.username))
                        )
                    )
                }
                return@setOnClickListener
            }
            loginViewModel.onUsernamePasswordSubmit(info, client, username, password)
            usernamePasswordContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }
}



