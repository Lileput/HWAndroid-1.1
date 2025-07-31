package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.viewModel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: PostViewModel by viewModels()
        viewModel.data.observe(this) { post ->
            with(binding) {
                author.text = post.author
                published.text = post.published
                content.text = post.content
                countEyes.text = ShortNumberFormatter.format(post.views)
                countLikes.text = ShortNumberFormatter.format(post.likes)
                countReposts.text = ShortNumberFormatter.format(post.share)
                if (post.likeByMe) {
                    likes.setImageResource(R.drawable.ic_liked_red_24)
                } else {
                    likes.setImageResource(R.drawable.ic_outline_like_24)
                }
//                countLikes.text = ShortNumberFormatter.format(post.likes)
            }
            binding.likes.setOnClickListener {
                viewModel.like()
            }

            binding.reposts.setOnClickListener {
                viewModel.reposts()
            }
        }

    }
}
