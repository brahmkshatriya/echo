package dev.brahmkshatriya.echo.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.isNightMode
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.loginNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var binding by autoCleared<FragmentLoginBinding>()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.iconContainer.updatePadding(top = it.top)
            binding.loading.root.applyContentInsets(it)
            binding.accountList.applyContentInsets(it)
            binding.loginContainer.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolbar.title = getString(R.string.extension_login, clientName)
        val client = loginViewModel.extensionList.getClient(clientId)
        if (client == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }
        client.metadata.iconUrl.load(binding.extensionIcon, R.drawable.ic_extension) {
            binding.extensionIcon.setImageDrawable(it)
        }
        when (client) {
            is LoginClient.WebView -> {
                binding.configureWebView(client)
            }

            else -> {
                createSnack(requireContext().loginNotSupported(clientName))
                parentFragmentManager.popBackStack()
            }
        }
        observe(loginViewModel.loginUsers) {
            it ?: return@observe

            binding.loading.root.isVisible = false
            binding.accountList.isVisible = true
        }
    }

    companion object {
        fun newInstance(clientId: String, clientName: String) = LoginFragment().apply {
            arguments = Bundle().apply {
                putString("clientId", clientId)
                putString("clientName", clientName)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun FragmentLoginBinding.configureWebView(
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
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                callback.isEnabled = webView.canGoBack()
                url ?: return
                if (url.contains(loginWebViewStopUrlRegex)) {
                    CookieManager.getInstance().run {
                        val cookies = getCookie(url)?.parseCookies() ?: emptyMap()
                        loginViewModel.onWebViewStop(client, cookies)
                        removeAllCookies(null)
                        flush()
                    }
                    webView.stopLoading()
                    webView.isVisible = false
                    loading.root.isVisible = true
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(loginWebViewInitialUrl.url, loginWebViewInitialUrl.headers)
    }

    @Suppress("DEPRECATION")
    private fun WebView.applyDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.isAlgorithmicDarkeningAllowed = true
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                settings,
                if(context.isNightMode()) WebSettingsCompat.FORCE_DARK_ON
                else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    fun String.parseCookies() = split(";").associate { cookie ->
        val (key, value) = cookie.split("=")
        key to value
    }
}


