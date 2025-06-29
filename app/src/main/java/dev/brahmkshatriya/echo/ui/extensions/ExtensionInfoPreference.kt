package dev.brahmkshatriya.echo.ui.extensions

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.ItemLoginUserBinding
import dev.brahmkshatriya.echo.databinding.PreferenceExtensionInfoBinding
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionInfoFragment.Companion.openLink
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.addOnDestroyObserver
import dev.brahmkshatriya.echo.utils.ui.SimpleItemSpan
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionInfoPreference(
    private val fragment: Fragment,
    private val extension: Extension<*>,
    private val isLoginClient: Boolean,
) : Preference(fragment.requireContext()) {

    private val viewModel by fragment.activityViewModel<ExtensionsViewModel>()
    private val loginViewModel by fragment.activityViewModel<LoginUserListViewModel>()

    init {
        layoutResource = R.layout.preference_extension_info
        if (isLoginClient) {
            loginViewModel.currentExtension.value = extension
            fragment.addOnDestroyObserver {
                loginViewModel.currentExtension.value = viewModel.extensionLoader.current.value
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val binding = PreferenceExtensionInfoBinding.bind(holder.itemView)
        val metadata = extension.metadata
        binding.extensionDetails.text =
            "${metadata.version} • ${metadata.importType.name}"

        val byAuthor = fragment.getString(R.string.by_x, metadata.author)
        val type = getType(extension.type)
        val typeString = fragment.getString(R.string.x_extension, fragment.getString(type))
        val span = SpannableString("$typeString\n\n${metadata.description}\n\n$byAuthor")
        val authUrl = metadata.authorUrl
        if (authUrl != null) {
            val itemSpan = SimpleItemSpan(context) {
                fragment.requireActivity().openLink(authUrl)
            }
            val start = span.length - metadata.author.length
            span.setSpan(itemSpan, start, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.extensionDescription.text = span
        binding.extensionDescription.movementMethod = LinkMovementMethod.getInstance()

        fun updateText(enabled: Boolean) {
            binding.extensionEnabledText.text = fragment.getString(
                if (enabled) R.string.enabled else R.string.disabled
            )
        }
        binding.extensionEnabledSwitch.apply {
            updateText(metadata.isEnabled)
            isChecked = metadata.isEnabled
            setOnCheckedChangeListener { _, isChecked ->
                updateText(isChecked)
                viewModel.setExtensionEnabled(extension.type, metadata.id, isChecked)
            }
            binding.extensionEnabled.setOnClickListener { toggle() }
        }
        if (isLoginClient) binding.extensionLoginUser.bind(fragment)
        else binding.extensionLoginUser.root.isVisible = false
    }

    companion object {
        fun getType(type: ExtensionType) = when (type) {
            ExtensionType.MUSIC -> R.string.music
            ExtensionType.TRACKER -> R.string.tracker
            ExtensionType.LYRICS -> R.string.lyrics
            ExtensionType.MISC -> R.string.misc
        }

        private fun ItemLoginUserBinding.bind(fragment: Fragment) = with(fragment) {
                val viewModel by activityViewModel<LoginUserListViewModel>()
                val binding = this@bind

                binding.switchAccount.setOnClickListener {
                    LoginUserListBottomSheet().show(parentFragmentManager, null)
                }
                observe(viewModel.currentUser) { user ->
                    binding.login.isVisible = user == null
                    binding.notLoggedInContainer.isVisible = user == null

                    binding.logout.isVisible = user != null
                    binding.userContainer.isVisible = user != null

                    val ext = viewModel.currentExtension.value
                    binding.login.setOnClickListener {
                        ext ?: return@setOnClickListener
                        requireActivity().openFragment<LoginFragment>(
                            null,
                            LoginFragment.getBundle(ext.id, ext.name, ext.type)
                        )
                    }

                    binding.logout.setOnClickListener {
                        ext ?: return@setOnClickListener
                        viewModel.logout(user?.toEntity(ext.type, ext.id))
                        viewModel.setLoginUser(CurrentUser(ext.type, ext.id, null))
                    }

                    binding.incognito.setOnClickListener {
                        ext ?: return@setOnClickListener
                        viewModel.setLoginUser(CurrentUser(ext.type, ext.id, null))
                    }

                    binding.currentUserName.text = user?.name
                    binding.currentUserSubTitle.text = user?.subtitle ?: ext?.name
                    user?.cover.loadInto(binding.currentUserAvatar, R.drawable.ic_account_circle)
                }
            }
    }
}