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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.SinglePostFragment.Companion.postId
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.PostLoadStateAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewModel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeetFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private var scrollObserver : RecyclerView.AdapterDataObserver? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val viewModel: PostViewModel by hiltNavGraphViewModels(R.id.nav_main)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.list.layoutManager = layoutManager

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun like(post: Post) {
                if (post.likedByMe) {
                    viewModel.unlikeWithCheck(post.id)
                } else {
                    viewModel.likeWithCheck(post.id)
                }
            }

            override fun remove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun repost(post: Post) {
                viewModel.reposts(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            override fun edit(post: Post) {
                findNavController().navigate(
                    R.id.action_feetFragment_to_newPostFragment,
                    Bundle().apply {
                        putString(NewPostFragment.EXTRA_EDIT_POST, post.content)
                        putLong(NewPostFragment.EXTRA_EDIT_POST_ID, post.id)
                    }
                )
            }

            override fun onPlayVideo(videoUrl: String) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, videoUrl.toUri()))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), R.string.no_video_app, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun showDeleteConfirmation(post: Post) {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.delete_post_confirmation)
                    .setPositiveButton(R.string.yes) { _, _ -> remove(post) }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }

            override fun onItemClick(post: Post) {
                findNavController().navigate(
                    R.id.action_feetFragment_to_singlePostFragment,
                    Bundle().apply {
                        postId = post.id
                    }
                )
            }

            override fun onImageClick(imageUrl: String) {
                findNavController().navigate(
                    R.id.action_feetFragment_to_photoViewFragment,
                    PhotoViewFragment.createArguments(imageUrl)
                )
            }
        })



        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostLoadStateAdapter { adapter.retry() },
            footer = PostLoadStateAdapter { adapter.retry() }
        )

        scrollObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.list.smoothScrollToPosition(0)
                    adapter.unregisterAdapterDataObserver(this)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.data.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
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
                binding.retry.isVisible = hasError && isEmpty
                binding.errorTitle.isVisible = hasError && isEmpty

                if (loadStates.append is LoadState.Error) {
                    val error = (loadStates.append as LoadState.Error).error
                    Snackbar.make(binding.root,
                        "Ошибка при загрузке: ${error.message}",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state.newPostsCount > 0) {
                binding.newPostsBanner.visibility = View.VISIBLE
                binding.newPostsCount.text = state.newPostsCount.toString()
            } else {
                binding.newPostsBanner.visibility = View.GONE
            }
        }

        binding.newPostsBanner.setOnClickListener {
            binding.newPostsBanner.visibility = View.GONE
            viewModel.showNewPosts()
            adapter.refresh()
            binding.list.smoothScrollToPosition(0)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Повторить") {
                        adapter.retry()
                    }
                    .setAnchorView(binding.ok)
                    .show()
            }
        }

        viewModel.shouldAuthenticate.observe(viewLifecycleOwner) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.auth_required)
                .setMessage(R.string.auth_required_message)
                .setPositiveButton(R.string.sign_in) { _, _ ->
                    findNavController().navigate(R.id.signInFragment)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        viewModel.shouldConfirmLogout.observe(viewLifecycleOwner) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_logout)
                .setMessage(R.string.confirm_logout_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    appAuth.clear()
                    findNavController().navigateUp()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
        }

        binding.retry.setOnClickListener {
            adapter.retry()
        }

        binding.ok.setOnClickListener {
            findNavController().navigate(R.id.action_feetFragment_to_newPostFragment)
        }

        return binding.root
    }
}