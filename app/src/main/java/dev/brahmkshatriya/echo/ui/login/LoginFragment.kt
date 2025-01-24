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
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.databinding.ItemInputBinding
import dev.brahmkshatriya.echo.extensions.getExtension
import dev.brahmkshatriya.echo.extensions.isClient
import dev.brahmkshatriya.echo.ui.exception.AppException
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.loginNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.set
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
            newInstance(error.extension.id, error.extension.name, error.extension.type)

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
    private val loginViewModel by viewModels<LoginViewModel>()

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
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.title = getString(R.string.extension_login, clientName)

        val extension = when (clientType) {
            ExtensionType.MUSIC -> loginViewModel.extensionList.getExtension(clientId)
            ExtensionType.TRACKER -> loginViewModel.trackerList.getExtension(clientId)
            ExtensionType.LYRICS -> loginViewModel.lyricsList.getExtension(clientId)
            ExtensionType.MISC -> loginViewModel.miscList.getExtension(clientId)
        }

        if (extension == null) {
            createSnack(requireContext().noClient())
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
                createSnack(requireContext().loginNotSupported(clientName))
                parentFragmentManager.popBackStack()
                return@launch
            }

            metadata.iconUrl?.toImageHolder()
                .loadAsCircle(binding.extensionIcon, R.drawable.ic_extension) {
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
                createSnack(requireContext().loginNotSupported(clientName))
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
        webView.webViewClient = object : WebViewClient() {

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = webView.canGoBack()
                url ?: return
                if (loginWebViewStopUrlRegex.find(url) != null) {
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
                        SnackBar.Message(
                            getString(R.string.required_field, getString(R.string.username))
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



