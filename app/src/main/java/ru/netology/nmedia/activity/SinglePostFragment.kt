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
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostViewHolder
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.LongArg
import ru.netology.nmedia.viewModel.PostViewModel

class SinglePostFragment: Fragment() {

    private val viewModel : PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSinglePostBinding.inflate(inflater, container, false)

        val postId = arguments?.postId ?: 0L

        viewModel.data.observe(viewLifecycleOwner) { posts ->
            val post = posts.posts.find { it.id == postId }
            post?.let {

                val viewHolder = PostViewHolder(binding.postSingle, object : OnInteractionListener {

                    override fun like(post: Post) {
                        if (post.likedByMe) {
                            viewModel.unlike(post.id)
                        } else {
                            viewModel.like(post.id)
                        }
                    }
                    override fun remove(post: Post) {
                        viewModel.removeById(post.id)
                        findNavController().navigateUp()
                    }
                    override fun repost(post: Post) = viewModel.reposts(post.id)

                    override fun edit(post: Post) {
                        findNavController().navigate(
                            R.id.action_singlePostFragment_to_newPostFragment,
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
                            Toast.makeText(requireContext(), R.string.no_video_app, Toast.LENGTH_SHORT).show()
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
                    }

                    override fun onImageClick(imageUrl: String) {
                        findNavController().navigate(
                            R.id.action_feetFragment_to_photoViewFragment,
                            PhotoViewFragment.createArguments(imageUrl)
                        )
                    }
                })

                viewHolder.bind(it)

                binding.postSingle.root.setOnClickListener(null)

            } ?: run {
                findNavController().navigateUp()
            }
        }

        return binding.root
    }

    companion object {
        var Bundle.postId: Long by LongArg
    }
}