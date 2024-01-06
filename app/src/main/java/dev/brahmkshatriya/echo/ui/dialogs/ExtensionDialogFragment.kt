package dev.brahmkshatriya.echo.ui.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import dev.brahmkshatriya.echo.databinding.ButtonExtensionBinding
import dev.brahmkshatriya.echo.databinding.DialogExtensionBinding


class ExtensionDialogFragment : DialogFragment() {

    private lateinit var binding: DialogExtensionBinding

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = DialogExtensionBinding.inflate(inflater, parent, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        setupWithNavController(binding.topAppBar, navController)

        (0 .. 5).forEach {
            val button = ButtonExtensionBinding
                .inflate(layoutInflater, binding.toggleButton, true)
                .root
            button.text = "Bruh"
            binding.toggleButton.addView(button)
            button.id = it
        }
        binding.toggleButton.check(1)
        binding.toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), checkedId.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        binding.addExtension.setOnClickListener {
            val button = ButtonExtensionBinding
                .inflate(layoutInflater, binding.toggleButton, true)
                .root
            button.text = "Bruh"
            binding.toggleButton.addView(button)
        }
    }
}