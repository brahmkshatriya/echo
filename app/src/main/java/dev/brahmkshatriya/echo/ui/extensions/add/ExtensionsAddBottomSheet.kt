package dev.brahmkshatriya.echo.ui.extensions.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogExtensionAddBinding
import dev.brahmkshatriya.echo.extensions.InstallationUtils.openFileSelector
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ExtensionsAddBottomSheet : BottomSheetDialogFragment() {

    var binding by autoCleared<DialogExtensionAddBinding>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogExtensionAddBinding.inflate(inflater)
        return binding.root
    }

    val viewModel by activityViewModel<AddViewModel>()
    private val extensionViewModel by activityViewModel<ExtensionsViewModel>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()
        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.installationTypeGroup.addOnButtonCheckedListener { group, _, _ ->
            val isLink = group.checkedButtonId == R.id.linkAdd
            binding.textInputLayout.isVisible = isLink
        }

        binding.editText.setOnEditorActionListener { _, _, _ ->
            binding.installButton.performClick()
            true
        }

        binding.installButton.setOnClickListener {
            val isLink = binding.installationTypeGroup.checkedButtonId == R.id.linkAdd
            if (!isLink) lifecycleScope.launch {
                val file = runCatching { requireActivity().openFileSelector() }.getOrNull()
                extensionViewModel.installWithPrompt(listOfNotNull(file))
                dismissAllowingStateLoss()
            } else {
                val link = binding.editText.text.toString()
                if (link.isEmpty()) return@setOnClickListener
                viewModel.addFromLinkOrCode(link)
            }
        }

        observe(viewModel.addingFlow) {
            val loading = it != AddViewModel.AddState.Init
            binding.loading.root.isVisible = loading
            binding.nestedScrollView.isVisible = !loading
            when (it) {
                AddViewModel.AddState.Init -> {}
                AddViewModel.AddState.Loading -> {
                    binding.loading.textView.text = context.getString(R.string.loading)
                }

                is AddViewModel.AddState.AddList -> if (it.list != null) {
                    if (it.list.isEmpty()) {
                        createSnack(R.string.list_is_empty)
                        dismiss()
                    } else {
                        if (viewModel.opened) return@observe
                        viewModel.opened = true
                        ExtensionsAddListBottomSheet().show(context.supportFragmentManager, null)
                    }
                } else dismiss()

                is AddViewModel.AddState.Downloading -> {
                    binding.loading.textView.text = context.getString(
                        R.string.downloading_x, it.item.name
                    )
                }

                is AddViewModel.AddState.Final -> {
                    viewModel.opened = false
                    viewModel.addingFlow.value = AddViewModel.AddState.Init
                    dismiss()
                }
            }
        }
    }
}