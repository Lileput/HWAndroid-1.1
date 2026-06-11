package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.UserAdapter
import ru.netology.nmedia.databinding.FragmentUsersBinding
import ru.netology.nmedia.viewModel.UsersViewModel

@AndroidEntryPoint
class UsersFragment : Fragment() {
    private val viewModel: UsersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentUsersBinding.inflate(inflater, container, false)
        val adapter = UserAdapter { user ->
            findNavController().navigate(
                R.id.profileFragment,
                Bundle().apply { putLong("userId", user.id) },
            )
        }

        binding.list.adapter = adapter

        viewModel.data.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            binding.empty.isVisible = users.isEmpty()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swipeRefreshLayout.isRefreshing = state.refreshing
            binding.errorGroup.isVisible = state.error

            if (state.error) {
                binding.list.isVisible = false
                binding.empty.isVisible = false
            } else {
                binding.list.isVisible = true
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.retry.setOnClickListener {
            viewModel.refresh()
        }

        return binding.root
    }
}