package dev.brahmkshatriya.echo.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogLoginUserBinding
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.ui.download.DownloadingFragment
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel

class LoginUserBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogLoginUserBinding>()
    val viewModel by activityViewModels<LoginUserViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.incognito.setOnClickListener {
            dismiss()
            viewModel.setLoginUser(null)
        }
        binding.settings.setOnClickListener {
            dismiss()
            requireActivity().openFragment(SettingsFragment())
        }
        binding.downloads.setOnClickListener {
            dismiss()
            requireActivity().openFragment(DownloadingFragment())
        }
        binding.switchAccount.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        observe(viewModel.currentUser) { (client, user) ->
            binding.login.isVisible = user == null
            binding.notLoggedInContainer.isVisible = user == null

            binding.logout.isVisible = user != null
            binding.userContainer.isVisible = user != null

            binding.login.setOnClickListener {
                client?.metadata?.run {
                    requireActivity().openFragment(LoginFragment.newInstance(id, name))
                }
                dismiss()
            }

            binding.logout.setOnClickListener {
                viewModel.logout(client?.metadata?.id, user)
                viewModel.setLoginUser(null)
            }

            binding.currentUserName.text = user?.name
            binding.currentUserSubTitle.text = client?.metadata?.name
            user?.cover.loadInto(binding.currentUserAvatar, R.drawable.ic_account_circle)
        }
    }
}