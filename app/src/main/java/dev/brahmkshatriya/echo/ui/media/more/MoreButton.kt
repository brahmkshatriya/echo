package dev.brahmkshatriya.echo.ui.media.more

import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil

data class MoreButton(
    val id: String, val title: String, val icon: Int, val onClick: () -> Unit
) {
    object DiffCallback : DiffUtil.ItemCallback<MoreButton>() {
        override fun areItemsTheSame(oldItem: MoreButton, newItem: MoreButton) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MoreButton, newItem: MoreButton) =
            oldItem == newItem
    }

    companion object {
        fun DialogFragment.button(
            id: String, title: String, icon: Int, onClick: () -> Unit
        ) = MoreButton(id, title, icon) {
            onClick()
            dismiss()
        }

        fun DialogFragment.button(
            id: String, title: Int, icon: Int, onClick: () -> Unit
        ) = button(id, getString(title), icon, onClick)
    }
}