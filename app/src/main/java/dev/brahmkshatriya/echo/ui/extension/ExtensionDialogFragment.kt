package dev.brahmkshatriya.echo.ui.extension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionBinding
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe


class ExtensionDialogFragment : DialogFragment() {

    private lateinit var binding: DialogExtensionBinding
    private val viewModel: ExtensionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionBinding.inflate(inflater, parent, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        setupWithNavController(binding.topAppBar, navController)

        binding.addExtension.isEnabled = false

        var oldListener: OnButtonCheckedListener? = null
        val extensionFlow = viewModel.getExtensionList()
        observe(extensionFlow) { list ->

            binding.buttonToggleGroup.removeAllViews()
            binding.progressIndicator.isVisible = list.isNullOrEmpty()
            if (list.isNullOrEmpty()) return@observe

            val map = list.mapIndexed { index, extension ->
                val button = ButtonExtensionBinding.inflate(
                        layoutInflater,
                        binding.buttonToggleGroup,
                        false
                    ).root
                val metadata = extension.metadata
                button.text = metadata.name
                binding.buttonToggleGroup.addView(button)
                metadata.iconUrl?.loadInto(button)
                button.id = index
                index to extension
            }.toMap()

            val checked = map.filter {
                it.value == viewModel.getCurrentExtension()
            }.keys.firstOrNull()

            if (checked != null) binding.buttonToggleGroup.check(checked)

            val listener = OnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) map[checkedId]?.let {
                    viewModel.setExtension(it)
                }
            }
            binding.buttonToggleGroup.run {
                oldListener?.let { removeOnButtonCheckedListener(it) }
                addOnButtonCheckedListener(listener)
                oldListener = listener
            }
        }
    }
}