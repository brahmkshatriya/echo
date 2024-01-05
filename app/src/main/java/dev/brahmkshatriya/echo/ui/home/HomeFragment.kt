package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentRecyclerBinding
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.utils.HeaderAdapter
import dev.brahmkshatriya.echo.ui.utils.ShimmerAdapter
import dev.brahmkshatriya.echo.ui.utils.autoCleared
import dev.brahmkshatriya.echo.ui.utils.dpToPx
import dev.brahmkshatriya.echo.ui.utils.updatePaddingWithSystemInsets

class HomeFragment : Fragment() {

    private var binding: FragmentRecyclerBinding by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentRecyclerBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerViewModel.handleBackPress(this)
        updatePaddingWithSystemInsets(binding.recyclerView)
        binding.swipeRefresh.setProgressViewOffset(true, 0, 72.dpToPx())

        val headerAdapter = HeaderAdapter(R.string.home)

        binding.recyclerView.adapter = ConcatAdapter(headerAdapter, ShimmerAdapter())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

    }
}