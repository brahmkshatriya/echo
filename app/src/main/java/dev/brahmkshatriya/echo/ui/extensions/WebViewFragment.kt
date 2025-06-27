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
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
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
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebViewFragment : Fragment() {

    private val vm by activityViewModel<ExtensionsViewModel>()
    private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
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
        if (savedInstanceState != null) {
            binding.root.restoreState(savedInstanceState)
            return
        }
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
            target: WebViewRequest<T>,
            skipTimeout: Boolean = false,
            onComplete: suspend (Result<T?>?) -> Unit
        ): OnBackPressedCallback? {
            val request = runCatching { target.initialUrl }.getOrNull()
                ?: return null
            val stopRegex = runCatching { target.stopUrlRegex }.getOrNull()
                ?: return null
            val timeout = runCatching { target.maxTimeout }.getOrNull()
                ?: return null

            val callback = object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            }
            val bridge = Bridge()
            val requests = mutableListOf<Request>()
            val timeoutJob = if (!skipTimeout) scope.launch {
                delay(timeout)
                stop(callback)
                onComplete(
                    Result.failure(
                        Exception(
                            "WebView request timed out after $timeout ms\nParsed Links:\n" +
                                    requests.joinToString("\n") { it.url }
                        )
                    )
                )
            } else null
            webViewClient = object : RequestInspectorWebViewClient(this@configure) {
                val client = OkHttpClient()
                override fun doUpdateVisitedHistory(
                    view: WebView?, url: String?, isReload: Boolean
                ) {
                    callback.isEnabled = canGoBack()
                }

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
                var done = false
                override fun shouldInterceptRequest(
                    view: WebView,
                    webViewRequest: com.acsbendi.requestinspectorwebview.WebViewRequest
                ): WebResourceResponse? {
                    println("Intercepted request: $webViewRequest")
                    val url = webViewRequest.url
                    val req = okhttp3.Request.Builder().url(webViewRequest.url)
                        .headers(
                            webViewRequest.headers.toHeaders().newBuilder()
                                .set("X-Requested-With", "")
                                .set("sec-ch-ua", "")
                                .build()
                        )
                        .method(
                            webViewRequest.method,
                            webViewRequest.body.takeIf { webViewRequest.method == "POST" }?.toByteArray()?.toRequestBody()
                        )
                        .build()
                    println("Intercepted request: ${req.url}")
                    println("Headers: ${req.headers.toMultimap()}")
                    val response = runBlocking(Dispatchers.IO) {
                        runCatching { client.newCall(req).execute() }
                    }.getOrNull()

                    val actualResponse = if (response != null) {
                        val type = response.header("content-type", "text/plain")
                        val (contentType, charset) = type?.split(";")?.map { it.trim() }
                            ?.let { it[0] to it.getOrNull(1)?.substringAfter("charset=") }
                            ?: ("text/plain" to null)
                        println("Content-Type: $type ($contentType, $charset)")
                        println("Response: ${response.headers.names()}")
                        WebResourceResponse(contentType, charset, response.body.byteStream())
                    } else null
                    if (done) return actualResponse
                    requests.add(webViewRequest.toRequest())
                    if (stopRegex.find(url) == null) return actualResponse
                    done = true
                    timeoutJob?.cancel()
                    scope.launch {
                        mutex.withLock {
                            onComplete(null)
                            val result = runCatching {
                                val headerRes = if (target is WebViewRequest.Headers)
                                    target.onStop(requests)
                                else null
                                val cookieRes = if (target is WebViewRequest.Cookie) {
                                    val cookie = CookieManager.getInstance().getCookie(url) ?: ""
                                    target.onStop(webViewRequest.toRequest(), cookie)
                                } else null
                                val evalRes = if (target is WebViewRequest.Evaluate)
                                    target.onStop(
                                        webViewRequest.toRequest(),
                                        evalJS(bridge, target.javascriptToEvaluate)
                                    )
                                else null
                                evalRes ?: cookieRes ?: headerRes
                            }
                            stop(callback)
                            onComplete(result)
                        }
                    }
                    return actualResponse
                }

//
//                override fun shouldOverrideUrlLoading(
//                    view: WebView?,
//                    request: WebResourceRequest?
//                ): Boolean {
//                    val headers = request?.requestHeaders ?: mutableMapOf()
//                    headers["X-Requested-With"] = ""
//                    view?.loadUrl(request?.url.toString(), headers)
//                    return true
//                }
            }


            settings.apply {
                domStorageEnabled = true
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                userAgentString = request.headers["User-Agent"] ?: USER_AGENT
                cacheMode =
                    if (runCatching { target.dontCache }.getOrNull() != true) WebSettings.LOAD_NO_CACHE
                    else WebSettings.LOAD_DEFAULT
            }

            addJavascriptInterface(bridge, "bridge")
            loadUrl(request.url, request.headers)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                settings.isAlgorithmicDarkeningAllowed = true
            }
            return callback
        }

        private fun com.acsbendi.requestinspectorwebview.WebViewRequest.toRequest(): Request {
            val cookie = CookieManager.getInstance().getCookie(url)
            return Request(url, buildMap {
                if (cookie != null) put("Cookie", cookie)
                putAll(headers)
            })
        }

        private suspend fun WebView.evalJS(bridge: Bridge?, js: String) =
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

        private suspend fun WebView.stop(
            callback: OnBackPressedCallback
        ) = withContext(Dispatchers.Main) {
            loadUrl("about:blank")
            callback.isEnabled = false
            clearCache(false)
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().run {
                removeAllCookies(null)
                flush()
            }
        }

        fun AppCompatActivity.onWebViewIntent(
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

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 2; Jeff Bezos) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
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

    class Hidden : Fragment(R.layout.fragment_webview) {
        private val vm by activityViewModel<ExtensionsViewModel>()
        private val webViewClient by lazy { vm.extensionLoader.webViewClientFactory }
        private val wrapper by lazy {
            val id = requireArguments().getInt("webViewRequest")
            webViewClient.requests[id] ?: throw IllegalStateException("Invalid webview request")
        }

        private fun removeSelf() {
            parentFragmentManager.commit(true) { remove(this@Hidden) }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val binding = FragmentWebviewBinding.bind(view)
            if (savedInstanceState != null) {
                binding.root.restoreState(savedInstanceState)
                return
            }
            binding.root.configure(vm.viewModelScope, wrapper.request) {
                webViewClient.responseFlow.emit(wrapper to it)
                if (it == null) return@configure
                runCatching { removeSelf() }
            } ?: removeSelf()
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
            addIfNull<WebViewFragment>(R.id.genericFragmentContainer, "webview", arguments)
        }
    }
}

