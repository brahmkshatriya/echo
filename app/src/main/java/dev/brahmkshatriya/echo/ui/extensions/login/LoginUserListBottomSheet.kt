package dev.brahmkshatriya.echo.ui.extensions.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogLoginUserListBinding
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LoginUserListBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogLoginUserListBinding>()
    val viewModel by activityViewModel<LoginUserListViewModel>()

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

        observe(viewModel.allUsers) { (extension, list) ->
            binding.accountListLoading.root.isVisible = list == null
            binding.accountListToggleGroup.isVisible = list != null
            binding.title.isVisible = extension != null

            extension ?: return@observe
            list ?: return@observe
            binding.title.text = getString(R.string.select_x_account, extension.name)
            binding.addAccount.setOnClickListener {
                dismiss()
                requireActivity().openFragment(
                    LoginFragment.newInstance(
                        extension.id, extension.name, ExtensionType.MUSIC
                    )
                )
            }
            binding.accountListToggleGroup.removeAllViews()
            val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, id, isChecked ->
                if (isChecked) {
                    val user = list[id]
                    binding.accountListLogin.isEnabled = true
                    binding.accountListLogin.setOnClickListener {
                        viewModel.setLoginUser(user.toEntity(extension.type, extension.id))
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