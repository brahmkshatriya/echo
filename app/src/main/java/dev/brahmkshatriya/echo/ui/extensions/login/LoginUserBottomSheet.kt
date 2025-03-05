package dev.brahmkshatriya.echo.ui.extensions.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogLoginUserBinding
import dev.brahmkshatriya.echo.databinding.ItemLoginUserBinding
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LoginUserBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogLoginUserBinding>()
    val viewModel by activityViewModel<LoginUserListViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settings.setOnClickListener {
            dismiss()
            requireActivity().openFragment<SettingsFragment>()
        }
        val extension = viewModel.currentExtension.value ?: return
        viewModel.currentExtension.value = extension
        binding.userContainer.bind(this, ::dismiss)
    }

    companion object {

        fun ItemLoginUserBinding.bind(fragment: Fragment, dismiss: () -> Unit) = with(fragment) {
            val viewModel by activityViewModel<LoginUserListViewModel>()
            val binding = this@bind

            binding.switchAccount.setOnClickListener {
                dismiss()
                LoginUserListBottomSheet().show(parentFragmentManager, null)
            }
            observe(viewModel.currentUser) { (extensionData, user) ->
                binding.login.isVisible = user == null
                binding.notLoggedInContainer.isVisible = user == null

                binding.logout.isVisible = user != null
                binding.userContainer.isVisible = user != null

                val metadata = extensionData?.metadata
                binding.login.setOnClickListener {
                    metadata?.run {
                        requireActivity().openFragment<LoginFragment>(
                            null,
                            LoginFragment.getBundle(id, name, extensionData.type)
                        )
                    }
                    dismiss()
                }

                binding.logout.setOnClickListener {
                    val meta = metadata ?: return@setOnClickListener
                    viewModel.logout(user?.toEntity(meta.type, meta.id))
                    viewModel.setLoginUser(CurrentUser(meta.type, meta.id, null))
                }

                binding.incognito.setOnClickListener {
                    dismiss()
                    val meta = metadata ?: return@setOnClickListener
                    viewModel.setLoginUser(CurrentUser(meta.type, meta.id, null))
                }

                binding.currentUserName.text = user?.name
                binding.currentUserSubTitle.text = user?.subtitle ?: metadata?.name
                user?.cover.loadInto(binding.currentUserAvatar, R.drawable.ic_account_circle)
            }
        }
    }
}