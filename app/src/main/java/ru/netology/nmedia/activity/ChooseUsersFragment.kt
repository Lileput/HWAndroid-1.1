package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.ChooseUserAdapter
import ru.netology.nmedia.databinding.FragmentChooseUsersBinding
import ru.netology.nmedia.viewModel.ChooseUsersViewModel

@AndroidEntryPoint
class ChooseUsersFragment : Fragment() {

    private val viewModel: ChooseUsersViewModel by viewModels()

    private var _binding: FragmentChooseUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChooseUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChooseUsersBinding.inflate(inflater, container, false)

        val initial = arguments?.getLongArray(ARG_SELECTED_IDS)?.toSet().orEmpty()
        viewModel.initSelection(initial)
        viewModel.loadUsers()

        adapter = ChooseUserAdapter(
            isSelected = viewModel::isSelected,
            onToggle = { user ->
                viewModel.toggle(user.id)
                adapter.notifyDataSetChanged()
            },
        )

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.choose_users)
            setDisplayHomeAsUpEnabled(true)
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_post_map, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.confirm -> {
                            val resultKey = arguments?.getString(ARG_RESULT_KEY)
                                ?: NewPostFragment.KEY_MENTION_IDS
                            findNavController().previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(resultKey, viewModel.selectedIds())
                            findNavController().navigateUp()
                            true
                        }
                        else -> false
                    }
            },
            viewLifecycleOwner,
        )

        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.error.isVisible = !error.isNullOrBlank()
            binding.error.text = error
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SELECTED_IDS = "selected_ids"
        const val ARG_RESULT_KEY = "result_key"
    }
}
