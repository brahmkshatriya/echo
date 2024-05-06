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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.plugger.getClient
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadWith
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.loginNotSupported
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel.Companion.noClient
import dev.brahmkshatriya.echo.viewmodels.SnackBar.Companion.createSnack
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val extension = loginViewModel.extensionList.getClient(clientId)
        if (extension == null) {
            createSnack(requireContext().noClient())
            parentFragmentManager.popBackStack()
            return
        }
        if (extension.client as? LoginClient == null) {
            createSnack(requireContext().loginNotSupported(clientName))
            parentFragmentManager.popBackStack()
            return
        }

        extension.metadata.iconUrl?.toImageHolder().loadWith(
            binding.extensionIcon, R.drawable.ic_extension
        ) {
            binding.extensionIcon.setImageDrawable(it)
        }

        binding.loginContainer.isVisible = true
        when (extension.client) {
            is LoginClient.WebView -> binding.configureWebView(extension.client)
            else -> createSnack(R.string.todo)
        }

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.popBackStack()
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
        webView.settings.javaScriptEnabled = true
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
        loadingContainer.root.isVisible = true
    }

}