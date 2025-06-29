package dev.brahmkshatriya.echo.ui.extensions.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogLoginUserListBinding
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
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
        binding.title.setNavigationOnClickListener { dismiss() }

        var listener: OnButtonCheckedListener? = null
        observe(viewModel.allUsers) { (ext, list) ->
            binding.accountListLoading.root.isVisible = ext == null
            binding.accountListToggleGroup.isVisible = ext != null
            binding.title.title = getString(R.string.select_x_account, ext?.name ?: "")
            binding.accountListToggleGroup.removeAllViews()
            listener?.let { binding.accountListToggleGroup.removeOnButtonCheckedListener(it) }

            ext ?: return@observe

            binding.title.setOnMenuItemClickListener {
                viewModel.setLoginUser(CurrentUser(ext.type, ext.id, null))
                true
            }

            binding.addAccount.setOnClickListener {
                dismiss()
                requireActivity().openFragment<LoginFragment>(
                    null,
                    LoginFragment.getBundle(ext.id, ext.name, ext.type)
                )
            }

            val selectedUser = viewModel.currentUser.value

            binding.logout.isEnabled = selectedUser != null
            binding.logout.setOnClickListener {
                viewModel.logout(selectedUser?.toEntity(ext.type, ext.id))
                viewModel.setLoginUser(CurrentUser(ext.type, ext.id, null))
            }

            list.forEachIndexed { index, (user, selected) ->
                val button = ButtonExtensionBinding.inflate(
                    layoutInflater, binding.accountListToggleGroup, false
                ).root
                button.text = user.name
                binding.accountListToggleGroup.addView(button)
                button.isChecked = selected
                user.cover.loadAsCircle(button) {
                    if (it != null) {
                        button.icon = it
                        button.iconTint = null
                    } else button.setIconResource(R.drawable.ic_account_circle)
                }
                button.id = index
            }

            val checked = list.indexOfFirst { it.second }.takeIf { it != -1 }
            if (checked != null) binding.accountListToggleGroup.check(checked)

            listener = OnButtonCheckedListener { _, id, isChecked ->
                if (isChecked) {
                    val user = list[id].first
                    viewModel.setLoginUser(user.toEntity(ext.type, ext.id))
                    dismiss()
                }
            }
            binding.accountListToggleGroup.addOnButtonCheckedListener(listener!!)
        }
    }
}