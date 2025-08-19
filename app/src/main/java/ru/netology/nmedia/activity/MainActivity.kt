package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewModel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()
        val newPostLauncher = registerForActivityResult(NewPostContract) { result ->
            result ?: return@registerForActivityResult
            val (text, postId) = result
            if (postId != 0L) {
                viewModel.edit(postId, text)
            } else {
                viewModel.save(text)
            }
        }
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
                newPostLauncher.launch(post)
            }

            override fun onPlayVideo(videoUrl: String) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, R.string.no_video_app, Toast.LENGTH_SHORT).show()
                }
            }

            override fun showDeleteConfirmation(post: Post) {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(R.string.delete_post_confirmation)
                    .setPositiveButton(R.string.yes) { _, _ -> remove(post) }
                    .setNegativeButton(R.string.no, null)
                    .show()
            }
        })

        binding.list.adapter = adapter
        viewModel.data.observe(this) { posts ->
            val new = posts.size > adapter.currentList.size && adapter.currentList.isNotEmpty()
            adapter.submitList(posts) {
                if (new) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }
        binding.ok.setOnClickListener{
            newPostLauncher.launch(null)
        }
    }
}