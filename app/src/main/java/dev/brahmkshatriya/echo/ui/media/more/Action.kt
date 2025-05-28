package dev.brahmkshatriya.echo.ui.media.more

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.R
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.databinding.ItemTextButtonBinding
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto

data class Action(
    val title: String,
    val image: Image,
    val onClick: () -> Unit
) {
    sealed interface Image
    data class ResourceImage(val drawable: Int) : Image
    data class CustomImage(val image: ImageHolder?, val placeholder: Int, val circle: Boolean) : Image

    object DiffCallback : DiffUtil.ItemCallback<Action>() {
        override fun areItemsTheSame(oldItem: Action, newItem: Action) =
            oldItem.image == newItem.image

        override fun areContentsTheSame(oldItem: Action, newItem: Action) = oldItem == newItem
    }

    class Adapter : ListAdapter<Action, Adapter.ViewHolder>(DiffCallback) {
        class ViewHolder(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            onClick: (Int) -> Unit,
            val binding: ItemTextButtonBinding =
                ItemTextButtonBinding.inflate(layoutInflater, parent, false)
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    onClick(bindingAdapterPosition)
                }
            }

            fun bind(action: Action?) {
                binding.textView.text = action?.title
                when (val img = action?.image) {
                    is CustomImage -> {
                        binding.imageView.imageTintList = null
                        if (img.circle) img.image.loadAsCircle(binding.root, img.placeholder) {
                            binding.imageView.setImageDrawable(it)
                        } else img.image.loadInto(binding.imageView, img.placeholder)
                    }

                    is ResourceImage -> {
                        binding.imageView.imageTintList = ColorStateList.valueOf(
                            MaterialColors.getColor(binding.root, R.attr.colorPrimary)
                        )
                        binding.imageView.setImageResource(img.drawable)
                    }

                    null -> binding.imageView.setImageDrawable(null)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater, parent, {
                getItemOrNull(it)?.onClick?.invoke()
            })
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        private fun getItemOrNull(position: Int) = runCatching { getItem(position) }.getOrNull()
    }

    companion object {
        fun DialogFragment.resource(drawable: Int, string: Int, action: () -> Unit): Action {
            return Action(getString(string), ResourceImage(drawable)) {
                action()
                dismiss()
            }
        }
    }
}

