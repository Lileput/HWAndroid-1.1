package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.adapter.onInteractionListener
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewModel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()

        val adapter = PostAdapter(object : onInteractionListener {
            override fun like(post: Post) {
                viewModel.like(post.id)
            }

            override fun remove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun repost(post: Post) {
                viewModel.reposts(post.id)
            }

            override fun edit(post: Post) {
                viewModel.edit(post)
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
        viewModel.edited.observe(this) { post ->
            if (post.id != 0L) {
                binding.editPanel.visibility = View.VISIBLE
                binding.editMessage.text = post.content
                binding.content.setText(post.content)
                binding.content.requestFocus()
                AndroidUtils.showKeyboard(binding.content)
            } else {
                binding.editPanel.visibility = View.GONE
            }
        }
        binding.cancelEdit.setOnClickListener {
            viewModel.cancelEdit()
            binding.content.setText("")
            binding.content.clearFocus()
            AndroidUtils.hideKeyboard(binding.content)
        }

        binding.save.setOnClickListener {
            val text = binding.content.text.toString()
            if (text.isBlank()) {
                Toast.makeText(this@MainActivity, R.string.error_empty_content, Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            viewModel.save(text)

            binding.content.setText("")
            binding.content.clearFocus()

            AndroidUtils.hideKeyboard(binding.content)
        }
    }
}
