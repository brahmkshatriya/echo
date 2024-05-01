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
        binding.logout.setOnClickListener {
            viewModel.setLoginUser(null)
            dismiss()
        }

        binding.settings.setOnClickListener {
            dismiss()
            requireActivity().openFragment(SettingsFragment())
        }
        binding.switchAccount.setOnClickListener {
            dismiss()
            LoginUserListBottomSheet().show(parentFragmentManager, null)
        }
        observe(viewModel.currentUser) { (client, user) ->
            binding.userLoginContainer.isVisible = user == null
            binding.userLogoutContainer.isVisible = user != null
            binding.switchAccount.isVisible = user != null

            binding.login.setOnClickListener {
                client?.metadata?.run {
                    openFragment(LoginFragment.newInstance(id, name))
                }
                dismiss()
            }

            binding.currentUserName.text = user?.name
            binding.currentUserSubTitle.text = client?.metadata?.name
            user?.cover.loadInto(binding.currentUserAvatar, R.drawable.ic_account_circle)
        }
    }
}