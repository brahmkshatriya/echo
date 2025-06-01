package dev.brahmkshatriya.echo.ui.extensions.login

import android.content.Context
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
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
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
import dev.brahmkshatriya.echo.extensions.exceptions.AppException
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.extensions.WebViewFragment.Companion.configure
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.addOnDestroyObserver
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
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
            appBarLayout.onAppBarChangeListener { offset ->
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

        private inline fun <reified T : Fragment> Fragment.add(args: Bundle? = null) {
            val loginViewModel by activityViewModel<LoginViewModel>()
            childFragmentManager.run {
                loginViewModel.loading.value = false
                commit {
                    setReorderingAllowed(true)
                    replace<T>(R.id.genericFragmentContainer, null, args)
                    if (fragments.size > 0) addToBackStack(null)
                }
            }
        }

        private fun Context.loginNotSupported(extName: String): String {
            val login = getString(R.string.login)
            return getString(R.string.x_is_not_supported_in_x, login, extName)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        parentFragmentManager.run {
            commit { setPrimaryNavigationFragment(this@LoginFragment) }
            addOnDestroyObserver { commit(true) { setPrimaryNavigationFragment(null) } }
        }
        binding.bind(this)
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolBar.title = getString(R.string.x_login, extName)

        val extension = loginViewModel.extensionLoader.extensions
            .getFlow(clientType).value?.find { it.id == extId }

        if (extension == null) {
            parentFragmentManager.popBackStack()
            return
        }

        val metadata = extension.metadata
        metadata.icon.loadAsCircle(binding.extensionIcon, R.drawable.ic_extension_48dp) {
            binding.extensionIcon.setImageDrawable(it)
        }

        observe(loginViewModel.loading) {
            binding.genericFragmentContainer.isVisible = !it
            binding.loading.root.isVisible = it
        }

        observe(loginViewModel.loadingOver) {
            parentFragmentManager.commitNow { setPrimaryNavigationFragment(null) }
            parentFragmentManager.popBackStack()
        }

        lifecycleScope.launch {
            val client = extension.instance.value().getOrNull() as? LoginClient
            if (client == null) {
                createSnack(requireContext().loginNotSupported(extName))
                parentFragmentManager.popBackStack()
                return@launch
            }

            val totalClients = listOfNotNull(
                if (client is LoginClient.WebView) 1 else 0,
                if (client is LoginClient.CustomInput) {
                    runCatching { client.forms }.getOrNull().orEmpty().size
                } else 0,
            ).sum()

            when (totalClients) {
                0 -> {
                    createSnack(requireContext().loginNotSupported(extName))
                    parentFragmentManager.popBackStack()
                    return@launch
                }

                1 -> when (client) {
                    is LoginClient.WebView -> add<WebView>(arguments)
                    is LoginClient.CustomInput -> add<CustomInput>(arguments)
                    else -> throw IllegalStateException("Unknown client type $client")
                }

                else -> add<Selector>(arguments)
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
            loginViewModel.extensionLoader.extensions.getFlow(clientType).value?.find { it.id == extId }
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
                    button to { requireParentFragment().add<WebView>(arguments) }
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
                            requireParentFragment().add<CustomInput>(
                                Bundle().apply {
                                    putAll(arguments)
                                    putInt("formIndex", index)
                                }
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
            loginViewModel.extensionLoader.extensions.getFlow(clientType).value?.find { it.id == extId }!!
        }
        private val webViewRequest by lazy {
            (extension.instance.value as LoginClient.WebView).webViewRequest
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            if (savedInstanceState == null) {
                setupTransition(view, useZ = false)
                val binding = FragmentWebviewBinding.bind(view)
                val callback =
                    binding.root.configure(loginViewModel.viewModelScope, webViewRequest) {
                        if (it == null) loginViewModel.loading.value = true
                        else loginViewModel.onWebViewStop(extension, it)
                    } ?: return
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
            } else {
                val binding = FragmentWebviewBinding.bind(view)
                binding.root.restoreState(savedInstanceState)
            }
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
            loginViewModel.extensionLoader.extensions.getFlow(clientType).value?.find { it.id == extId }!!
        }
        private val formIndex by lazy { requireArguments().getInt("formIndex", 0) }
        private val form by lazy {
            (extension.instance.value as LoginClient.CustomInput).forms.getOrNull(formIndex)
                ?: throw IllegalStateException("No form found for extension ${extension.id}")
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            setupTransition(view, useZ = false)
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