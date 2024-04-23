package dev.brahmkshatriya.echo.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import dev.brahmkshatriya.echo.databinding.FragmentMainBinding
import dev.brahmkshatriya.echo.ui.home.HomeFragment
import dev.brahmkshatriya.echo.ui.library.LibraryFragment
import dev.brahmkshatriya.echo.ui.search.SearchFragment
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.utils.startAnimation
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

class MainFragment : Fragment() {

    var binding by autoCleared<FragmentMainBinding>()
    val viewModel by activityViewModels<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        val adapter = MainAdapter(this)
        binding.root.adapter = adapter
        binding.root.setPageTransformer(PageTransformer())
        binding.root.isUserInputEnabled = false
        observe(viewModel.navigation) {
            binding.root.setCurrentItem(it, false)
        }
    }

    class MainAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> SearchFragment()
                2 -> LibraryFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }

    inner class PageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            if (position == 0.0f) {
                view.translationY = view.height / 4f
                view.alpha = 0f
                startAnimation(
                    view, view.animate().translationY(0f).alpha(1f), 0.5f
                )
            }
        }
    }


    companion object {
        fun RecyclerView.first() =
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        fun RecyclerView.scrollTo(position: Int, block: (Int) -> Unit) = doOnLayout {
            if (position < 1) return@doOnLayout
            (layoutManager as LinearLayoutManager).run {
                scrollToPositionWithOffset(position, 0)
                post { block(findFirstVisibleItemPosition()) }
            }
        }
    }
}