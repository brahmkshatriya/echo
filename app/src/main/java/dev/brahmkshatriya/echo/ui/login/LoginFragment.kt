package dev.brahmkshatriya.echo.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import dev.brahmkshatriya.echo.plugger.getExtension
import dev.brahmkshatriya.echo.utils.autoCleared
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
        }
        else null

    private fun createWebViewClient(client: LoginClient) =
        if (client is LoginClient.WebView) object : LoginClient.WebView {
            override val loginWebViewInitialUrl = client.loginWebViewInitialUrl
            override val loginWebViewStopUrlRegex = client.loginWebViewStopUrlRegex
            override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
            override suspend fun onLoginWebviewStop(url: String, cookie: String) =
                client.onLoginWebviewStop(url, cookie)
        }
        else null

    private fun createCustomTextInputClient(client: LoginClient) =
        if (client is LoginClient.CustomTextInput) object : LoginClient.CustomTextInput {
            override val loginInputFields = client.loginInputFields
            override suspend fun onSetLoginUser(user: User?) = client.onSetLoginUser(user)
            override suspend fun onLogin(data: Map<String, String?>) = client.onLogin(data)
        }
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

        val map = mapOf(
            LoginClient.UsernamePassword::class to binding.loginUserPass,
            LoginClient.WebView::class to binding.loginWebview,
            LoginClient.CustomTextInput::class to binding.loginCustomInput
        )

        val clients = listOfNotNull(
            createUsernamePasswordClient(client),
            createWebViewClient(client),
            createCustomTextInputClient(client)
        ).associateWith { map[it::class]!! }

        if (clients.size == 1) loginViewModel.loginClient.value = clients.keys.first()
        else {
            binding.loginToggleGroup.isVisible = true
            clients.forEach { (loginClient, button) ->
                button.setOnClickListener {
                    loginViewModel.loginClient.value = loginClient
                }
            }
        }
        observe(loginViewModel.loginClient) { loginClient ->
            loginClient ?: return@observe
            binding.loginToggleGroup.isVisible = false
            when (loginClient) {
                is LoginClient.WebView -> binding.configureWebView(loginClient)
                is LoginClient.CustomTextInput -> binding.configureCustomTextInput(loginClient)
                is LoginClient.UsernamePassword -> binding.configureUsernamePassword(loginClient)
            }
        }

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun FragmentLoginBinding.configureWebView(
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
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url ?: return
                if (loginWebViewStopUrlRegex.matches(url)) {
                    val cookie = CookieManager.getInstance().getCookie(url)
                    loginViewModel.onWebViewStop(clientId, client, url, cookie)
                    afterWebViewStop()
                    callback.isEnabled = false
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = webView.canGoBack()
            }
        }
        webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            userAgentString = loginWebViewInitialUrl.headers["User-Agent"]
        }
        webView.loadUrl(loginWebViewInitialUrl.url, loginWebViewInitialUrl.headers)

        lifecycleScope.launch {
            delay(1000)
            appBarLayout.setExpanded(false, true)
        }
    }

    private fun WebView.applyDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.isAlgorithmicDarkeningAllowed = true
        }
    }

    private fun FragmentLoginBinding.afterWebViewStop() {
        webView.stopLoading()
        CookieManager.getInstance().run {
            removeAllCookies(null)
            flush()
        }
        webViewContainer.isVisible = false
        loadingContainer.root.isVisible = true
    }

    private fun FragmentLoginBinding.configureCustomTextInput(
        client: LoginClient.CustomTextInput
    ) {
        customInputContainer.isVisible = true
        client.loginInputFields.forEach { field ->
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
            loginViewModel.onCustomTextInputSubmit(clientId, client)
            customInputContainer.isVisible = false
            loadingContainer.root.isVisible = true
        }
    }

    private fun FragmentLoginBinding.configureUsernamePassword(
        client: LoginClient.UsernamePassword
    ) {
        usernamePasswordContainer.isVisible = true
        loginUsername.requestFocus()
        loginUsername.setOnEditorActionListener { _, _, _ ->
            loginPassword.requestFocus()
            true
        }
        loginPassword.setOnEditorActionListener { _, _, _ ->
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
                return@setOnEditorActionListener true
            }
            loginViewModel.onUsernamePasswordSubmit(clientId, client, username, password)
            usernamePasswordContainer.isVisible = false
            loadingContainer.root.isVisible = true
            true
        }
    }
}



