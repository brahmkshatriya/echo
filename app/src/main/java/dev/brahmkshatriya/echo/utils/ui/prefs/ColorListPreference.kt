package dev.brahmkshatriya.echo.utils.ui.prefs

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemColorCardBinding

class ColorListPreference(val fragment: Fragment, var listener: Listener? = null) :
    Preference(fragment.requireContext()) {

    fun interface Listener {
        fun onColorSelected(color: Int)
    }

    private lateinit var colorCardAdapter: ColorCardAdapter
    private lateinit var sharedPreferences: SharedPreferences

    init {
        layoutResource = R.layout.preference_color_list
    }

    private var defaultValue: Int? = null
    override fun onSetInitialValue(defaultValue: Any?) {
        this.defaultValue = defaultValue as? Int
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val view: View = holder.itemView
        view.alpha = if (isEnabled) 1f else 0.66f

        sharedPreferences = preferenceManager.sharedPreferences!!
        val addColorCard: MaterialCardView = view.findViewById(R.id.addColor)
        val recentColorsRecyclerView: RecyclerView = view.findViewById(R.id.recentColors)

        colorCardAdapter = ColorCardAdapter(getSavedColors(), adapterListener)
        recentColorsRecyclerView.adapter = colorCardAdapter

        addColorCard.setOnClickListener {
            if (!isEnabled) return@setOnClickListener
            ColorPickerDialog().show(fragment.parentFragmentManager, null)
        }

        fragment.parentFragmentManager
            .setFragmentResultListener("colorPicker", fragment) { _, bundle ->
                val color = bundle.getInt("color").takeIf { it != -1 }
                    ?: return@setFragmentResultListener
                addColor(color)
                colorCardAdapter.colors = getSavedColors()
                colorCardAdapter.notifyDataSetChanged()
            }
    }

    private val adapterListener = object : ColorCardAdapter.Listener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onColorSelected(color: Int) {
            if (!isEnabled) return
            saveSelectedColor(color)
            colorCardAdapter.colors = getSavedColors()
            colorCardAdapter.notifyDataSetChanged()
            listener?.onColorSelected(color)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onColorRemoved(color: Int): Boolean {
            if (!isEnabled) return false
            removeColor(color)
            colorCardAdapter.colors = getSavedColors()
            colorCardAdapter.notifyDataSetChanged()
            return true
        }
    }

    class ColorCardAdapter(var colors: List<Pair<Boolean, Int>>, val listener: Listener) :
        RecyclerView.Adapter<ColorCardAdapter.ViewHolder>() {
        interface Listener {
            fun onColorSelected(color: Int)
            fun onColorRemoved(color: Int): Boolean
        }

        inner class ViewHolder(val binding: ItemColorCardBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    listener.onColorSelected(colors[bindingAdapterPosition].second)
                }
                binding.root.setOnLongClickListener {
                    listener.onColorRemoved(colors[bindingAdapterPosition].second)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemColorCardBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = colors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val color = colors[position]
            holder.binding.root.setCardBackgroundColor(color.second)
            holder.binding.colorSelected.isVisible = color.first
        }

    }

    private fun getSelectedColor(): Int? {
        val color = sharedPreferences.getInt(key, defaultValue ?: -1)
        return color.takeIf { it != -1 }
    }

    private fun saveSelectedColor(color: Int?) {
        sharedPreferences.edit {
            putInt(key, color ?: -1)
        }
    }

    private fun getSavedColors(): List<Pair<Boolean, Int>> {
        val selectedColor = getSelectedColor()
        val colorString = sharedPreferences.getString("${key}_colors", "")
            ?.split(",").orEmpty()
            .map { it.toIntOrNull() } - selectedColor
        val colors = colorString.mapNotNull { it?.let { false to it } }
        return listOfNotNull(selectedColor?.let { true to it }) + colors
    }

    private fun List<Pair<Boolean, Int>>.inString() = joinToString(",") { it.second.toString() }

    private fun addColor(color: Int) {
        val colorList = getSavedColors().toMutableList()
        colorList.add(false to color)
        sharedPreferences.edit {
            putString("${key}_colors", colorList.inString())
        }
    }

    private fun removeColor(color: Int) {
        val colorList = getSavedColors().toMutableList()
        colorList.removeAll { it.second == color }
        sharedPreferences.edit {
            putString("${key}_colors", colorList.inString())
        }
    }

}