package dev.brahmkshatriya.echo.ui.extensions.login

import android.os.Bundle
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_URI
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LoginClient.InputField.Type
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.FragmentExtensionLoginCustomInputBinding
import dev.brahmkshatriya.echo.databinding.FragmentExtensionLoginSelectorBinding
import dev.brahmkshatriya.echo.databinding.FragmentGenericCollapsableBinding
import dev.brahmkshatriya.echo.databinding.FragmentWebviewBinding
import dev.brahmkshatriya.echo.databinding.ItemInputBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtension
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.getExtensionOrThrow
import dev.brahmkshatriya.echo.extensions.exceptions.AppException
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.extensions.WebViewUtils.configure
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.collections.set

class LoginFragment : Fragment() {
    companion object {
        fun getBundle(extId: String, extName: String, extensionType: ExtensionType) = bundleOf(
            "extId" to extId,
            "extName" to extName,
            "extensionType" to extensionType.name,
        )

        fun getBundle(error: AppException.LoginRequired) =
            getBundle(error.extension.id, error.extension.name, error.extension.type)


        fun FragmentGenericCollapsableBinding.bind(fragment: Fragment) = with(fragment) {
            setupTransition(root)
            applyInsets {
                genericFragmentContainer.applyContentInsets(it, 0)
            }
            applyBackPressCallback()
            appBarLayout.configureAppBar { offset ->
                toolbarOutline.alpha = offset
                iconContainer.alpha = 1 - offset
            }
            toolBar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
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

    private var binding by autoCleared<FragmentGenericCollapsableBinding>()
    private val clientType by lazy {
        val type = requireArguments().getString("extensionType")!!
        ExtensionType.valueOf(type)
    }
    private val extId by lazy { requireArguments().getString("extId")!! }
    private val extName by lazy { requireArguments().getString("extName")!! }
    private val loginViewModel by activityViewModel<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenericCollapsableBinding.inflate(inflater, container, false)
        return binding.root
    }

    private inline fun <reified T : Fragment> add(args: Bundle? = null) {
        if (!isAdded) return
        childFragmentManager.run {
            loginViewModel.loading.value = false
            commit {
                setReorderingAllowed(true)
                if (fragments.size > 0) addToBackStack(null)
                replace<T>(R.id.genericFragmentContainer, null, args)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.bind(this)
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolBar.title = getString(R.string.x_login, extName)

        val extension = loginViewModel.extensionLoader.getFlow(clientType).getExtension(extId)
        extension?.metadata?.icon.loadAsCircle(view, R.drawable.ic_extension_48dp) {
            binding.extensionIcon.setImageDrawable(it)
        }

        observe(loginViewModel.loading) {
            binding.genericFragmentContainer.isVisible = !it
            binding.loading.root.isVisible = it
        }

        observe(loginViewModel.loadingOver) {
            repeat(childFragmentManager.backStackEntryCount) {
                parentFragmentManager.popBackStack()
            }
            parentFragmentManager.popBackStack()
        }

        observe(loginViewModel.addFragmentFlow) {
            when (it) {
                LoginViewModel.FragmentType.Selector -> add<Selector>(arguments)
                LoginViewModel.FragmentType.WebView -> add<WebView>(arguments)
                is LoginViewModel.FragmentType.CustomInput -> add<CustomInput>(Bundle().apply {
                    putAll(arguments)
                    putInt("formIndex", it.index ?: 0)
                })
            }
        }

        if (childFragmentManager.fragments.isEmpty()) lifecycleScope.launch {
            lifecycle.withStarted {
                loginViewModel.loadClient(extension)
            }
        }
    }

    class Selector : Fragment(R.layout.fragment_extension_login_selector) {
        private val clientType by lazy {
            val type = requireArguments().getString("extensionType")!!
            ExtensionType.valueOf(type)
        }
        private val extId by lazy { requireArguments().getString("extId")!! }
        private val loginViewModel by activityViewModel<LoginViewModel>()
        private val extension by lazy {
            loginViewModel.extensionLoader.getFlow(clientType).getExtension(extId)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            setupTransition(view)
            val binding = FragmentExtensionLoginSelectorBinding.bind(view)
            val client = extension?.instance?.value
            val clients = listOfNotNull(
                if (client is LoginClient.WebView) {
                    val button = ButtonExtensionBinding.inflate(
                        layoutInflater, binding.loginToggleGroup, false
                    ).root
                    button.text = getString(R.string.webview)
                    button.setIconResource(R.drawable.ic_language)
                    button to { loginViewModel.changeFragment(LoginViewModel.FragmentType.WebView) }
                } else null,
                *(if (client is LoginClient.CustomInput) {
                    val forms = runCatching { client.forms }.getOrNull().orEmpty()
                    forms.mapIndexed { index, it ->
                        val button = ButtonExtensionBinding.inflate(
                            layoutInflater, binding.loginToggleGroup, false
                        ).root
                        button.text = it.label
                        button.setIconResource(getIcon(it.icon))
                        button to {
                            loginViewModel.changeFragment(
                                LoginViewModel.FragmentType.CustomInput(index)
                            )
                        }
                    }
                } else listOf()).toTypedArray(),
            )
            clients.forEachIndexed { index, pair ->
                val button = pair.first
                button.setOnClickListener { pair.second() }
                binding.loginToggleGroup.addView(button)
                button.id = index
            }
        }
    }

    class WebView : Fragment(R.layout.fragment_webview) {
        private val clientType by lazy {
            val type = requireArguments().getString("extensionType")!!
            ExtensionType.valueOf(type)
        }
        private val extId by lazy { requireArguments().getString("extId")!! }
        private val loginViewModel by activityViewModel<LoginViewModel>()
        private val extension by lazy {
            loginViewModel.extensionLoader.getFlow(clientType).getExtensionOrThrow(extId)
        }
        private val webViewRequest by lazy {
            (extension.instance.value as? LoginClient.WebView)?.webViewRequest
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            setupTransition(view, useZ = false)
            val binding = FragmentWebviewBinding.bind(view)
            val req = webViewRequest ?: return
            val callback = requireActivity().configure(binding.root, req, true) {
                if (it == null) loginViewModel.loading.value = true
                else loginViewModel.onWebViewStop(extension, it)
            }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }
    }

    class CustomInput : Fragment(R.layout.fragment_extension_login_custom_input) {
        private val clientType by lazy {
            val type = requireArguments().getString("extensionType")!!
            ExtensionType.valueOf(type)
        }
        private val extId by lazy { requireArguments().getString("extId")!! }
        private val loginViewModel by activityViewModel<LoginViewModel>()
        private val extension by lazy {
            loginViewModel.extensionLoader.getFlow(clientType).getExtensionOrThrow(extId)
        }
        private val formIndex by lazy { requireArguments().getInt("formIndex", 0) }
        private val form by lazy {
            (extension.instance.value as? LoginClient.CustomInput)?.forms?.getOrNull(formIndex)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            setupTransition(view, useZ = false)
            val form = form ?: run {
                message(Message("No form found for extension ${extension.id}"))
                parentFragmentManager.popBackStack()
                return
            }
            val binding = FragmentExtensionLoginCustomInputBinding.bind(view)
            binding.run {
                form.inputFields.forEachIndexed { index, field ->
                    val input = ItemInputBinding.inflate(
                        layoutInflater, customInput, false
                    )
                    input.root.id = field.key.hashCode()
                    input.editText.id = "${field.key}_input".hashCode()
                    input.root.hint = field.label
                    input.root.setStartIconDrawable(getIcon(field.type))
                    @Suppress("DEPRECATION")
                    input.root.isPasswordVisibilityToggleEnabled = field.type == Type.Password
                    input.editText.inputType = when (field.type) {
                        Type.Email -> TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        Type.Password -> TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                        Type.Number -> TYPE_CLASS_TEXT or TYPE_CLASS_NUMBER
                        Type.Url -> TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_URI
                        else -> TYPE_CLASS_TEXT
                    }
                    input.editText.setText(loginViewModel.inputs[field.key])
                    input.editText.doAfterTextChanged { editable ->
                        loginViewModel.inputs[field.key] =
                            editable.toString().takeIf { it.isNotBlank() }
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
                }
            }
        }

        private fun message(m: Message) {
            lifecycleScope.launch {
                loginViewModel.messageFlow.emit(m)
            }
        }
    }
}