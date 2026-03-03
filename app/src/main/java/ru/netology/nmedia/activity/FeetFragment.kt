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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.SinglePostFragment.Companion.postId
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.viewModel.PostViewModel

class FeetFragment : Fragment() {

    private var scrollObserver : RecyclerView.AdapterDataObserver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val viewModel: PostViewModel by viewModels(
            ownerProducer = ::requireParentFragment
        )

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

        binding.list.adapter = adapter

        scrollObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.list.smoothScrollToPosition(0)
                    adapter.unregisterAdapterDataObserver(this)
                }
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { feedModel ->
            adapter.submitList(feedModel.posts)
            binding.empty.isVisible = feedModel.empty
            binding.list.isVisible = !feedModel.empty
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshLayout.isRefreshing = state.refreshing
            binding.progress.isVisible = state.loading

            val currentFeedModel = viewModel.data.value

            val showError = state.error && (currentFeedModel?.empty == true)
            binding.errorGroup.isVisible = showError
            binding.retry.isVisible = showError
            binding.errorTitle.isVisible = showError

            binding.ok.isVisible = !state.error || (currentFeedModel?.empty == false)

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

            scrollObserver?.let { adapter.registerAdapterDataObserver(it) }
        }


        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Повторить") {
                        viewModel.load()
                    }
                    .setAnchorView(binding.ok)
                    .show()
            }
        }

        viewModel.newerCount.observe(viewLifecycleOwner) {
            println(it)
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
                    AppAuth.getInstance().clear()
                    findNavController().navigateUp()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.retry.setOnClickListener {
            viewModel.load()
        }

        binding.ok.setOnClickListener {
            findNavController().navigate(R.id.action_feetFragment_to_newPostFragment)
        }

        return binding.root
    }
}