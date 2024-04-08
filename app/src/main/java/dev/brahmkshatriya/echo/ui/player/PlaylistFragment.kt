package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

class PlaylistFragment : Fragment() {

    private var binding by autoCleared<FragmentPlaylistBinding>()
    private val viewModel by activityViewModels<PlayerViewModel>()
    private val uiViewModel by activityViewModels<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveQueueItems(toPos, fromPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.removeQueueItem(pos)
            }
        }
        val touchHelper = ItemTouchHelper(callback)
        val adapter = PlaylistAdapter(viewModel.currentFlow, object : PlaylistAdapter.Callback() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {
                emit(viewModel.audioIndexFlow) { position }
            }

            override fun onItemClosedClicked(position: Int) {
                viewModel.removeQueueItem(position)
            }
        })

        binding.root.adapter = adapter
        touchHelper.attachToRecyclerView(binding.root)

        observe(viewModel.listChangeFlow) {
            adapter.submitList(it)
        }

        observe(uiViewModel.infoSheetState) {
            if (it == STATE_DRAGGING || it == STATE_SETTLING) return@observe
            binding.root.isNestedScrollingEnabled = it != STATE_COLLAPSED
        }
    }
}