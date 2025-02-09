package dev.brahmkshatriya.echo.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogLoginUserListBinding
import dev.brahmkshatriya.echo.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.openFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.image.loadAsCircle
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.LoginUserViewModel

class LoginUserListBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogLoginUserListBinding>()
    val viewModel by activityViewModels<LoginUserViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLoginUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.accountListLogin.isEnabled = false

        observe(viewModel.allUsers) { (metadata, list) ->
            binding.accountListLoading.root.isVisible = list == null
            binding.accountListToggleGroup.isVisible = list != null
            metadata ?: return@observe
            list ?: return@observe
            binding.addAccount.setOnClickListener {
                dismiss()
                requireActivity().openFragment(
                    LoginFragment.newInstance(
                        metadata.id, metadata.name, ExtensionType.MUSIC
                    )
                )
            }
            binding.accountListToggleGroup.removeAllViews()
            val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, id, isChecked ->
                if (isChecked) {
                    val user = list[id]
                    binding.accountListLogin.isEnabled = true
                    binding.accountListLogin.setOnClickListener {
                        viewModel.setLoginUser(user.toEntity(metadata.id))
                        dismiss()
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
                user.cover.loadAsCircle(button) {
                    if (it != null) {
                        button.icon = it
                        button.iconTint = null
                    } else button.setIconResource(R.drawable.ic_account_circle)
                }
                button.id = index
            }
        }
    }

}