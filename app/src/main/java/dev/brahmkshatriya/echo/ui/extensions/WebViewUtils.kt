package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.WebViewRequest
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Request
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.databinding.FragmentWebviewBinding
import dev.brahmkshatriya.echo.extensions.WebViewClientFactory
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment.Companion.bind
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WebViewUtils {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    fun <T> FragmentActivity.configure(
        webView: WebView,
        target: WebViewRequest<T>,
        skipTimeout: Boolean,
        onComplete: suspend (Result<T>?) -> Unit
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                webView.goBack()
            }
        }
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().run {
            removeAllCookies(null)
            flush()
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = USER_AGENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isAlgorithmicDarkeningAllowed = true
        }
        runCatching {
            webView.load(lifecycleScope, callback, target, skipTimeout, onComplete)
        }.getOrElse {
            lifecycleScope.launch {
                webView.stop(callback)
                onComplete(Result.failure(it))
            }
        }
        return callback
    }

    private fun <T> WebView.load(
        scope: CoroutineScope,
        callback: OnBackPressedCallback,
        target: WebViewRequest<T>,
        skipTimeout: Boolean,
        onComplete: suspend (Result<T>?) -> Unit
    ) {
        val stopRegex = target.stopUrlRegex
        val timeout = target.maxTimeout
        val bridge = Bridge()
        val requests = mutableListOf<Request>()
        val timeoutJob = if (!skipTimeout) scope.launch {
            delay(timeout)
            onComplete(
                Result.failure(
                    Exception(
                        "WebView request timed out after $timeout ms\nParsed Links:\n" +
                                requests.joinToString("\n") { it.url }
                    )
                )
            )
        } else null
        webViewClient = object : RequestInspectorWebViewClient(this@load) {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                callback.isEnabled = canGoBack()
            }

            var done = false
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (target !is WebViewRequest.Evaluate) return
                if (done) return
                target.javascriptToEvaluateOnPageStart?.let { js ->
                    scope.launch {
                        runCatching { evalJS(null, js) }.onFailure {
                            stop(callback)
                            onComplete(Result.failure(it))
                        }
                    }
                }
            }

            val mutex = Mutex()
            fun intercept(
                request: Request
            ) {
                if (target is WebViewRequest.Headers) requests.add(request)
                if (stopRegex.find(request.url) == null) return
                done = true
                timeoutJob?.cancel()
                scope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        onComplete(null)
                        val result = runCatching {
                            val headerRes = if (target is WebViewRequest.Headers)
                                target.onStop(requests)
                            else null
                            val cookieRes = if (target is WebViewRequest.Cookie) {
                                val cookie =
                                    CookieManager.getInstance().getCookie(request.url) ?: ""
                                target.onStop(request, cookie)
                            } else null
                            val evalRes = if (target is WebViewRequest.Evaluate)
                                target.onStop(
                                    request,
                                    evalJS(bridge, target.javascriptToEvaluate)
                                )
                            else null
                            evalRes ?: cookieRes ?: headerRes!!
                        }
                        stop(callback)
                        onComplete(result)
                    }
                }
            }

            override fun shouldInterceptRequest(
                view: WebView, webViewRequest: com.acsbendi.requestinspectorwebview.WebViewRequest
            ): WebResourceResponse? {
                intercept(webViewRequest.run { Request(url, headers) })
                return null
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                request?.run {
                    val headers = requestHeaders?.toMutableMap() ?: mutableMapOf()
                    val cookie = CookieManager.getInstance().getCookie(url.toString())
                    if (cookie != null) headers["Cookie"] = cookie
                    intercept(Request(url.toString(), headers))
                }
                return false
            }
        }

        addJavascriptInterface(bridge, "bridge")
        settings.cacheMode =
            if (runCatching { target.dontCache }.getOrNull() == true) WebSettings.LOAD_NO_CACHE
            else WebSettings.LOAD_DEFAULT
        target.initialUrl.run {
            settings.userAgentString = headers["user-agent"] ?: settings.userAgentString
            loadUrl(url, headers)
        }
    }

    suspend fun WebView.evalJS(bridge: Bridge?, js: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine {
            bridge?.onResult = it::resume
            bridge?.onError = it::resumeWithException
            val asyncFunction = if (js.startsWith("async function")) js
            else if (js.startsWith("function")) "async $js"
            else {
                it.resumeWithException(Exception("Invalid JS function, must start with async or function"))
                return@suspendCancellableCoroutine
            }

            val newJs = """
        (function() {
            try {
                const fun = $asyncFunction;
                fun().then((result) => {
                    ${if (bridge != null) "bridge.putJsResult(result);" else ""}
                }).catch((error) => {
                    ${if (bridge != null) "bridge.putJsError(error.message || error.toString());" else ""}
                });
            } catch (error) {
                ${if (bridge != null) "bridge.putJsError(error.message || error.toString());" else ""}
            }
        })()
        """.trimIndent()
            evaluateJavascript(newJs, null)

            it.invokeOnCancellation {
                evaluateJavascript("javascript:window.stop();", null)
            }
        }
    }

    suspend fun WebView.stop(
        callback: OnBackPressedCallback
    ) = withContext(Dispatchers.Main) {
        loadUrl("about:blank")
        callback.isEnabled = false
    }

    @Suppress("unused")
    class Bridge {
        var onError: ((Throwable) -> Unit)? = null
        var onResult: ((String?) -> Unit)? = null

        @JavascriptInterface
        fun putJsResult(result: String?) {
            onResult?.invoke(result)
        }

        @JavascriptInterface
        fun putJsError(error: String?) {
            onError?.invoke(Exception(error ?: "Unknown JavaScript error"))
        }
    }

    fun FragmentActivity.onWebViewIntent(
        intent: Intent,
        webViewClient: WebViewClientFactory
    ) {
        val id = intent.getIntExtra("webViewRequest", -1)
        if (id == -1) return
        val wrapper = webViewClient.requests[id] ?: return
        createSnack(Message(getString(R.string.opening_webview_x, wrapper.reason)))
        if (wrapper.showWebView) openFragment<WithAppbar>(null, getBundle(id))
        else supportFragmentManager.commit {
            add<Hidden>(R.id.hiddenWebViewContainer, null, getBundle(id))
        }
    }

    private fun getBundle(id: Int) = Bundle().apply {
        putInt("webViewRequest", id)
    }

    class Hidden : Fragment(R.layout.fragment_webview) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
        private val wrapper by lazy {
            val id = requireArguments().getInt("webViewRequest")
            webViewClient.requests[id]
        }
        private val shouldRemove by lazy {
            requireArguments().getBoolean("hidden", true)
        }

        private fun removeSelf() {
            if (shouldRemove) parentFragmentManager.commit(true) { remove(this@Hidden) }
            else parentFragment?.parentFragmentManager?.popBackStack()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentWebviewBinding.bind(view)
            val wrapper = wrapper ?: run {
                removeSelf()
                return
            }
            val callback = requireActivity().configure(binding.root, wrapper.request, false) {
                webViewClient.responseFlow.emit(wrapper to it)
                if (it == null) runCatching { removeSelf() }
            }
            if (!shouldRemove) {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
                applyBackPressCallback()
            }
        }
    }

    class WithAppbar : Fragment(R.layout.fragment_generic_collapsable) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
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
            addIfNull<Hidden>(R.id.genericFragmentContainer, "webview", arguments?.apply {
                putBoolean("hidden", false)
            })
        }
    }
}

