package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.PostLikerAdapter
import ru.netology.nmedia.databinding.FragmentPostLikersBinding
import ru.netology.nmedia.viewModel.PostLikersViewModel

@AndroidEntryPoint
class PostLikersFragment : Fragment() {

    private val viewModel: PostLikersViewModel by viewModels()

    private var _binding: FragmentPostLikersBinding? = null
    private val binding get() = _binding!!

    private val adapter = PostLikerAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPostLikersBinding.inflate(inflater, container, false)

        val titleRes = arguments?.getInt(ARG_TITLE_RES, R.string.likers) ?: R.string.likers
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(titleRes)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        val userIds = arguments?.getLongArray(ARG_USER_IDS) ?: longArrayOf()
        viewModel.load(userIds)

        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_USER_IDS = "user_ids"
        const val ARG_TITLE_RES = "title_res"
    }
}
