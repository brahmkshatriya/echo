package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.databinding.FragmentWebviewBinding
import dev.brahmkshatriya.echo.extensions.WebViewClientImpl
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment.Companion.bind
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebViewFragment : Fragment() {

    private val vm by activityViewModel<ExtensionsViewModel>()
    private val webViewClient by lazy { vm.extensionLoader.webViewClient }
    private val wrapper by lazy {
        val id = requireArguments().getInt("webViewRequest")
        webViewClient.requests[id] ?: throw IllegalStateException("Invalid webview request")
    }

    private var binding by autoCleared<FragmentWebviewBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val callback = binding.root.configure(vm.viewModelScope, wrapper.request) {
            webViewClient.responseFlow.emit(wrapper to it)
            parentFragment?.parentFragmentManager?.popBackStack()
        } ?: run { parentFragment?.parentFragmentManager?.popBackStack(); return }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        applyBackPressCallback()
    }

    companion object {
        private fun getBundle(id: Int) = Bundle().apply {
            putInt("webViewRequest", id)
        }

        fun <T : Any> WebView.configure(
            scope: CoroutineScope,
            webViewRequest: WebViewRequest<T>,
            onComplete: suspend (Result<T?>?) -> Unit
        ): OnBackPressedCallback? {
            val request = runCatching { webViewRequest.initialUrl }.getOrNull()
                ?: return null
            val stopRegex = runCatching { webViewRequest.stopUrlRegex }.getOrNull()
                ?: return null

            val callback = object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            }
            val bridge = Bridge()
            val requests =
                if (webViewRequest is WebViewRequest.Headers) mutableListOf<Request>() else null
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(
                    view: WebView?, url: String?, isReload: Boolean
                ) {
                    callback.isEnabled = canGoBack()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (webViewRequest !is WebViewRequest.Evaluate) return
                    webViewRequest.javascriptToEvaluateOnPageStart?.let { js ->
                        scope.launch {
                            runCatching { evalJS(null, js) }.onFailure {
                                stop(callback, true)
                                onComplete(Result.failure(it))
                            }
                        }
                    }
                }

                val mutex = Mutex()
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    requests?.add(request.toRequest())
                    if (stopRegex.find(url) == null) return null

                    scope.launch {
                        mutex.withLock {
                            stop(callback, false)
                            onComplete(null)
                            val result = runCatching {
                                val headerRes = if (webViewRequest is WebViewRequest.Headers)
                                    webViewRequest.onStop(requests!!)
                                else null
                                val cookieRes = if (webViewRequest is WebViewRequest.Cookie) {
                                    val cookie = CookieManager.getInstance().getCookie(url) ?: ""
                                    webViewRequest.onStop(request.toRequest(), cookie)
                                } else null
                                val evalRes = if (webViewRequest is WebViewRequest.Evaluate)
                                    webViewRequest.onStop(
                                        request.toRequest(),
                                        evalJS(bridge, webViewRequest.javascriptToEvaluate)
                                    )
                                else null
                                evalRes ?: cookieRes ?: headerRes
                            }
                            stop(callback, true)
                            onComplete(result)
                        }
                    }
                    return null
                }
            }

            settings.apply {
                domStorageEnabled = true
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                userAgentString = request.headers["User-Agent"] ?: USER_AGENT
            }

            addJavascriptInterface(bridge, "bridge")
            loadUrl(request.url, request.headers)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                settings.isAlgorithmicDarkeningAllowed = true
            }
            return callback
        }

        private fun WebResourceRequest.toRequest() = Request(url.toString(), requestHeaders)

        private suspend fun WebView.evalJS(bridge: Bridge?, js: String) =
            suspendCancellableCoroutine {
                bridge?.onResult = it::resume
                val asyncFunction = if (js.startsWith("async function")) js
                else if (js.startsWith("function")) "async $js"
                else {
                    it.resumeWithException(Exception("Invalid JS function, must start with async or function"))
                    return@suspendCancellableCoroutine
                }
                val newJs = """
                (function() {
                    const fun = $asyncFunction;
                    fun().then((result) => { ${if (bridge != null) "bridge.putJsResult(result);" else ""} });
                })()
                """.trimIndent()
                evaluateJavascript(newJs, null)
                it.invokeOnCancellation {
                    evaluateJavascript("javascript:window.stop();", null)
                }
            }

        private suspend fun WebView.stop(
            callback: OnBackPressedCallback,
            clear: Boolean
        ) = withContext(Dispatchers.Main) {
            loadUrl("about:blank")
            callback.isEnabled = false
            if (!clear) return@withContext
            clearCache(false)
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().run {
                removeAllCookies(null)
                flush()
            }
        }

        fun AppCompatActivity.onWebViewIntent(
            intent: Intent,
            webViewClient: WebViewClientImpl,
            hiddenWebView: WebView,
        ) {
            val id = intent.getIntExtra("webViewRequest", -1)
            if (id == -1) return
            val wrapper = webViewClient.requests[id] ?: return
            createSnack(Message(getString(R.string.opening_webview_x, wrapper.reason)))

            if (wrapper.showWebView) openFragment<WithAppbar>(null, getBundle(id))
            else {
                val extensionsViewModel by viewModel<ExtensionsViewModel>()
                hiddenWebView.configure(extensionsViewModel.viewModelScope, wrapper.request) {
                    webViewClient.responseFlow.emit(wrapper to it)
                }
            }
        }

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
    }

    class Bridge {
        var onResult: ((String?) -> Unit)? = null

        @Suppress("unused")
        @JavascriptInterface
        fun putJsResult(result: String?) {
            onResult?.invoke(result)
        }
    }

    class WithAppbar : Fragment(R.layout.fragment_generic_collapsable) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClient }
        private val wrapper by lazy {
            val id = requireArguments().getInt("webViewRequest")
            webViewClient.requests[id] ?: throw IllegalStateException("Invalid webview request")
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentGenericCollapsableBinding.bind(view)
            binding.bind(this)
            binding.toolBar.title = wrapper.extension.name
            wrapper.extension.icon.loadAsCircle(
                binding.extensionIcon, R.drawable.ic_extension_48dp
            ) {
                binding.extensionIcon.setImageDrawable(it)
            }
            addIfNull<WebViewFragment>(R.id.genericFragmentContainer, "webview", arguments)
        }
    }
}

