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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.SinglePostFragment.Companion.postId
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewModel.PostViewModel

class FeetFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val viewModel: PostViewModel by viewModels(
            ownerProducer = ::requireParentFragment
        )

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun like(post: Post) {
                viewModel.like(post.id)
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
        })

        binding.list.adapter = adapter
        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val new = posts.size > adapter.currentList.size && adapter.currentList.isNotEmpty()
            adapter.submitList(posts) {
                if (new) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }
        binding.ok.setOnClickListener {
            findNavController().navigate(R.id.action_feetFragment_to_newPostFragment)
        }
        return binding.root
    }
}