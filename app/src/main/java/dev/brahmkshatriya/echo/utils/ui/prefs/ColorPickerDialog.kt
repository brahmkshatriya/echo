package dev.brahmkshatriya.echo.utils.ui.prefs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.madrapps.pikolo.listeners.OnColorSelectionListener
import dev.brahmkshatriya.echo.databinding.DialogColorPickerBinding
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import kotlin.random.Random

class ColorPickerDialog : DialogFragment() {

    var binding by autoCleared<DialogColorPickerBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    var currentColor: Int? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.colorPickerView.setColorSelectionListener(object : OnColorSelectionListener {
            override fun onColorSelected(color: Int) {
                currentColor = color
                binding.colorCard.setCardBackgroundColor(color)
            }

            override fun onColorSelectionEnd(color: Int) {}
            override fun onColorSelectionStart(color: Int) {}
        })
        binding.addColorButton.setOnClickListener { dismiss() }
        binding.randomColorButton.setOnClickListener { setColor(generateRandomColor()) }
        setColor(generateRandomColor())
    }

    private fun setColor(color: Int){
        currentColor = color
        binding.colorCard.setCardBackgroundColor(color)
        binding.colorPickerView.setColor(color)
    }

    private fun generateRandomColor(): Int {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        return Color.rgb(red, green, blue)
    }

    override fun onDestroy() {
        super.onDestroy()
        parentFragmentManager.setFragmentResult("colorPicker", Bundle().apply {
            putInt("color", currentColor!!)
        })
    }
}