package dev.brahmkshatriya.echo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import dev.brahmkshatriya.echo.databinding.FragmentPlaylistBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils
import dev.brahmkshatriya.echo.playback.Radio
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

class QueueFragment : Fragment() {

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

        var queueAdapter: PlaylistAdapter? = null
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (viewHolder.bindingAdapter != queueAdapter) return false
                if (target.bindingAdapter != queueAdapter) return false

                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                viewModel.moveQueueItems(toPos, fromPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                viewModel.removeQueueItem(pos)
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return if (viewHolder.bindingAdapter == queueAdapter) makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.START
                ) else 0
            }
        }
        val touchHelper = ItemTouchHelper(callback)

        queueAdapter = PlaylistAdapter(object : PlaylistAdapter.Callback() {
            override fun onDragHandleTouched(viewHolder: RecyclerView.ViewHolder) {
                touchHelper.startDrag(viewHolder)
            }

            override fun onItemClicked(position: Int) {
                viewModel.play(position)
            }

            override fun onItemClosedClicked(position: Int) {
                viewModel.removeQueueItem(position)
            }
        })

        val radioAdapter = PlaylistAdapter(object : PlaylistAdapter.Callback() {
            override fun onItemClicked(position: Int) {
                viewModel.radioPlay(position)
            }
        }, true)

        val radioLoaderAdapter = PlaylistAdapter.Loader()
        binding.root.adapter = ConcatAdapter(queueAdapter, radioLoaderAdapter, radioAdapter)
        touchHelper.attachToRecyclerView(binding.root)

        fun submit() {
            val current = viewModel.currentFlow.value
            val currentIndex = current?.index
            val it = viewModel.list.mapIndexed { index, mediaItem ->
                if (currentIndex == index) true to current.mediaItem
                else false to mediaItem
            }
            queueAdapter.submitList(it)
        }

        observe(viewModel.currentFlow) { submit() }
        observe(viewModel.listUpdateFlow) { submit() }

        observe(viewModel.radioStateFlow) { state ->
            radioLoaderAdapter.setLoading(state is Radio.State.Loading)
            val list = if (state is Radio.State.Loaded) state.tracks.drop(state.played + 1).map {
                false to MediaItemUtils.build(null, it, state.clientId, null)
            } else emptyList()
            radioAdapter.submitList(list)
        }

        val manager = binding.root.layoutManager as LinearLayoutManager
        val offset = 24.dpToPx(requireContext())
        observe(uiViewModel.changeInfoState) {
            val index = viewModel.currentFlow.value?.index ?: -1
            if (it == STATE_EXPANDED && index != -1)
                manager.scrollToPositionWithOffset(index, offset)
        }
    }
}