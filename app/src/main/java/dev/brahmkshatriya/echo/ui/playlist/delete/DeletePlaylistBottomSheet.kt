package dev.brahmkshatriya.echo.ui.playlist.delete

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.ItemLoadingBinding
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class DeletePlaylistBottomSheet : BottomSheetDialogFragment(R.layout.item_loading) {

    companion object {
        fun show(
            activity: FragmentActivity, extensionId: String, item: Playlist, loaded: Boolean = false
        ): AlertDialog = with(activity) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirmation))
                .setMessage(getString(R.string.delete_playlist_confirmation, item.title))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    newInstance(extensionId, item, loaded).show(supportFragmentManager, null)
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }

        private fun newInstance(
            extensionId: String, item: Playlist, loaded: Boolean
        ): DeletePlaylistBottomSheet {
            return DeletePlaylistBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("extensionId", extensionId)
                    putSerialized("item", item)
                    putBoolean("loaded", loaded)
                }
            }
        }
    }

    val args by lazy { requireArguments() }
    val extensionId by lazy { args.getString("extensionId")!! }
    val item by lazy { args.getSerialized<Playlist>("item")!! }
    val loaded by lazy { args.getBoolean("loaded", false) }

    val vm by viewModel<DeletePlaylistViewModel> {
        parametersOf(extensionId, item, loaded)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ItemLoadingBinding.bind(view)
        observe(vm.deleteStateFlow) { state ->
            val result = vm.playlistFlow.value
            val playlist = result?.getOrNull()
            val string = when (state) {
                is DeleteState.Deleted -> {
                    if (state.result.isSuccess) {
                        createSnack(getString(R.string.deleted_x, playlist?.title))
                        parentFragmentManager.setFragmentResult(
                            "deleted", bundleOf("id" to playlist?.id)
                        )
                        parentFragmentManager.setFragmentResult("reloadLibrary", Bundle.EMPTY)
                    }
                    dismiss()
                    return@observe
                }

                DeleteState.Deleting ->
                    getString(R.string.deleting_x, playlist?.title)

                DeleteState.Initial ->
                    getString(R.string.loading_x, playlist?.title)
            }
            binding.textView.text = string
        }
    }
}