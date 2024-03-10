package dev.brahmkshatriya.echo.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.clients.UserClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.databinding.FragmentArtistBinding
import dev.brahmkshatriya.echo.player.PlayerViewModel
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.ui.MediaItemClickListener
import dev.brahmkshatriya.echo.ui.adapters.MediaItemsContainerAdapter
import dev.brahmkshatriya.echo.ui.extension.ExtensionViewModel
import dev.brahmkshatriya.echo.ui.extension.getAdapterForExtension
import dev.brahmkshatriya.echo.ui.snackbar.SnackBarViewModel
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.loadInto
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets
import kotlinx.coroutines.flow.combine

class ArtistFragment : Fragment() {

    private val args: ArtistFragmentArgs by navArgs()

    private var binding: FragmentArtistBinding by autoCleared()

    private val viewModel: ArtistViewModel by viewModels()
    private val extensionViewModel: ExtensionViewModel by activityViewModels()
    private val snackBarViewModel: SnackBarViewModel by activityViewModels()
    private val playerViewModel: PlayerViewModel by activityViewModels()

    private val clickListener = MediaItemClickListener(this)
    private val mediaItemsContainerAdapter = MediaItemsContainerAdapter(this, clickListener)
    private val header = ArtistHeaderAdapter(object : ArtistHeaderAdapter.ArtistHeaderListener {

        override fun onSubscribeClicked(
            artist: Artist.Full,
            subscribe: Boolean,
            adapter: ArtistHeaderAdapter
        ) {
            adapter.submitSubscribe(subscribe)
        }

        override fun onRadioClicked(artist: Artist.Full) {
        }

    })
    private val concatAdapter = ConcatAdapter(header, mediaItemsContainerAdapter)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentArtistBinding.inflate(inflater, container, false)
        enterTransition = MaterialFade()
        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.recyclerView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.albumCoverContainer.updatePadding(top = insets.top)
            windowInsets
        }
        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            val inverted = 1 - offset
            binding.endIcon.alpha = inverted
            binding.toolbarOutline.alpha = offset
        }
        binding.toolbar.setupWithNavController(findNavController())
        postponeEnterTransition()
        binding.recyclerView.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        val artist: Artist.Small = args.artistWithCover ?: args.artistSmall ?: return

        binding.root.transitionName = artist.uri.toString()
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }
        binding.toolbar.title = artist.name.trim()

        (artist as? Artist.WithCover).let {
            it?.cover.loadInto(binding.albumCover, R.drawable.art_artist)
        }

        observe(extensionViewModel.extensionFlow) {
            binding.recyclerView.adapter = getAdapterForExtension<ArtistClient>(
                it, R.string.artist, concatAdapter, true
            ) { client ->
                if (client == null) return@getAdapterForExtension
                viewModel.loadArtist(client, snackBarViewModel.mutableExceptionFlow, artist)
            }
        }
        val headerFlow = viewModel.artistFlow
            .combine(extensionViewModel.extensionFlow) { it, client -> it to client }
        observe(headerFlow) { (artist, client) ->
            if (artist != null && client != null) {
                header.submit(artist, client is UserClient, client is RadioClient)
            }
        }
        observe(viewModel.result) {
            if (it != null) mediaItemsContainerAdapter.submit(it)
        }
    }
}