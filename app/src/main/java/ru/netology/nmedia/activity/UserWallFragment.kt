package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.SinglePostFragment.Companion.postId
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostLoadStateAdapter
import ru.netology.nmedia.databinding.FragmentUserWallBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewModel.ProfileViewModel

@AndroidEntryPoint
class UserWallFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    private var _binding: FragmentUserWallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserWallBinding.inflate(inflater, container, false)

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun like(post: Post) {
                if (post.likedByMe) viewModel.unlike(post.id) else viewModel.like(post.id)
            }

            override fun remove(post: Post) = viewModel.removeById(post.id)

            override fun repost(post: Post) {
                viewModel.repost(post.id)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_post)))
            }

            override fun edit(post: Post) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_newPostFragment,
                    Bundle().apply {
                        putString(NewPostFragment.EXTRA_EDIT_POST, post.content)
                        putLong(NewPostFragment.EXTRA_EDIT_POST_ID, post.id)
                    },
                )
            }

            override fun onPlayVideo(videoUrl: String) {
                ru.netology.nmedia.util.MediaPlaybackHelper.play(requireContext(), videoUrl)
            }

            override fun showDeleteConfirmation(post: Post) {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.delete_post_confirmation)
                    .setPositiveButton(R.string.yes) { _, _ -> viewModel.removeById(post.id) }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }

            override fun onItemClick(post: Post) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_singlePostFragment,
                    Bundle().apply { postId = post.id },
                )
            }

            override fun onImageClick(imageUrl: String) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_photoViewFragment,
                    PhotoViewFragment.createArguments(imageUrl),
                )
            }
        })

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostLoadStateAdapter { adapter.retry() },
            footer = PostLoadStateAdapter { adapter.retry() },
        )

        lifecycleScope.launch {
            viewModel.wallData.collectLatest { adapter.submitData(it) }
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                binding.swipeRefreshLayout.isRefreshing = loadStates.refresh is LoadState.Loading
                val isEmpty = adapter.itemCount == 0
                val hasError = loadStates.refresh is LoadState.Error
                binding.empty.isVisible = isEmpty && !hasError && loadStates.refresh !is LoadState.Loading
                binding.list.isVisible = !isEmpty
                binding.progress.isVisible = loadStates.refresh is LoadState.Loading && isEmpty
                binding.errorGroup.isVisible = hasError && isEmpty
                if (loadStates.refresh is LoadState.Error) {
                    val error = (loadStates.refresh as LoadState.Error).error
                    binding.errorTitle.text = error.message ?: getString(R.string.network_error)
                } else {
                    binding.errorTitle.setText(R.string.network_error)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener { adapter.refresh() }
        binding.retry.setOnClickListener { adapter.retry() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: Long): UserWallFragment = UserWallFragment().apply {
            arguments = bundleOf(ARG_USER_ID to userId)
        }
    }
}
