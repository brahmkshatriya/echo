package dev.brahmkshatriya.echo.ui.home

import android.os.Bundle
import android.view.View
import dev.brahmkshatriya.echo.BaseFragment
import dev.brahmkshatriya.echo.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateRootPadding()

    }
}