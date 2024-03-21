package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentRecyclerBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.adapters.ContainerLoadingAdapter
import dev.brahmkshatriya.echo.ui.adapters.HeaderAdapter
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class LibraryFragment : Fragment() {

    private var binding: FragmentRecyclerBinding by autoCleared()
    private val headerAdapter = HeaderAdapter(R.string.library)

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        binding = FragmentRecyclerBinding.inflate(inflater, parent, false)
        enterTransition = MaterialFade()
        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it)
        }
        binding.swipeRefresh.setProgressViewOffset(true, 0, 72.dpToPx())

        binding.recyclerView.adapter = ConcatAdapter(headerAdapter, ContainerLoadingAdapter())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

    }
}