package dev.brahmkshatriya.echo.ui.search

import android.os.Bundle
import android.view.View
import dev.brahmkshatriya.echo.BaseFragment
import dev.brahmkshatriya.echo.databinding.FragmentSearchBinding

class SearchFragment : BaseFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateRootBottomMargin()
    }

}