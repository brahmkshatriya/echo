package dev.brahmkshatriya.echo.ui.library

import android.os.Bundle
import android.view.View
import dev.brahmkshatriya.echo.BaseFragment
import dev.brahmkshatriya.echo.databinding.FragmentLibraryBinding

class LibraryFragment : BaseFragment<FragmentLibraryBinding>(FragmentLibraryBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateRootPadding()

    }
}