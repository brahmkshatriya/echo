package dev.brahmkshatriya.echo.ui.player.upnext

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.databinding.FragmentPlayerQueueBinding
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoClearedNullable
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class QueueFragment : Fragment() {

    private var binding by autoClearedNullable<FragmentPlayerQueueBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerQueueBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    private val queueAdapter by lazy {
        QueueAdapter(object : QueueAdapter.Listener() {
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
    }

    private val touchHelper by lazy {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
                viewModel.moveQueueItems(fromPos, toPos)
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
                return makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.START
                )
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view, false)
        val recyclerView = binding!!.root
        recyclerView.adapter = queueAdapter
        touchHelper.attachToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager as LinearLayoutManager
        val screenHeight = view.resources.displayMetrics.heightPixels / 3

        fun submit() {
            val current = viewModel.playerState.current.value
            val currentIndex = current?.index
            val it = viewModel.queue.mapIndexed { index, mediaItem ->
                if (currentIndex == index) current.isPlaying to current.mediaItem
                else null to mediaItem
            }
            queueAdapter.submitList(it) {
                currentIndex ?: return@submitList
                binding?.root?.scrollToPosition(currentIndex)
            }
        }

        observe(viewModel.playerState.current) { submit() }
        observe(viewModel.queueFlow) { submit() }

        val index = viewModel.playerState.current.value?.index ?: return
        manager.scrollToPositionWithOffset(index + 1, screenHeight)
    }
}