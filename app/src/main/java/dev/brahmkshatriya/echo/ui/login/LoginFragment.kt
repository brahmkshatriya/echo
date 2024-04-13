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
import com.google.android.material.button.MaterialButtonToggleGroup
import dagger.hilt.android.AndroidEntryPoint
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.FragmentLoginBinding
import dev.brahmkshatriya.echo.utils.Animator.setupTransition
import dev.brahmkshatriya.echo.utils.autoCleared
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
            binding.accountList.applyContentInsets(it)
            binding.accountListConfirmContainer.applyInsets(it)
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
        if (client as? LoginClient == null) {
            createSnack(requireContext().loginNotSupported(clientName))
            parentFragmentManager.popBackStack()
            return
        }

        client.metadata.iconUrl.load(binding.extensionIcon, R.drawable.ic_extension) {
            binding.extensionIcon.setImageDrawable(it)
        }

        binding.loginContainer.isVisible = true
        when (client) {
            is LoginClient.WebView -> binding.configureWebView(client)
            else -> createSnack(R.string.todo)
        }

        observe(loginViewModel.loginUsers) { list ->
            list ?: return@observe
            binding.accountListLoading.root.isVisible = false
            binding.accountListConfirmContainer.isVisible = true
            binding.accountListConfirm.isEnabled = false

            binding.accountListToggleGroup.removeAllViews()
            val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, id, isChecked ->
                if (isChecked) {
                    val user = list[id]
                    binding.accountListConfirm.isEnabled = true
                    binding.accountListConfirm.setOnClickListener {
                        loginViewModel.onUserSelected(client, user)
                        parentFragmentManager.popBackStack()
                    }
                }
            }
            binding.accountListToggleGroup.addOnButtonCheckedListener(listener)
            list.forEachIndexed { index, user ->
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater, binding.accountListToggleGroup, false
                ).root
                button.text = user.name
                binding.accountListToggleGroup.addView(button)
                user.cover.load(button, R.drawable.ic_account_circle) { button.icon = it }
                button.id = index
            }
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
                    loginViewModel.onWebViewStop(client, url, cookie)
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
        loginContainer.isVisible = false
        accountList.isVisible = true
    }

}