    package ru.netology.nmedia.activity

    import android.os.Bundle
    import androidx.activity.viewModels
    import androidx.appcompat.app.AppCompatActivity
    import ru.netology.nmedia.R
    import ru.netology.nmedia.adapter.PostAdapter
    import ru.netology.nmedia.databinding.ActivityMainBinding
    import ru.netology.nmedia.databinding.CardPostBinding
    import ru.netology.nmedia.dto.Post
    import ru.netology.nmedia.viewModel.PostViewModel

    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val viewModel: PostViewModel by viewModels()

            val adapter = PostAdapter(
                onItemLikeListener = { post: Post ->
                    viewModel.like(post.id)
                },
                onItemShareListener = { post: Post ->
                    viewModel.reposts(post.id)
                }
            )

            binding.list.adapter = adapter
            viewModel.data.observe(this) { posts ->
                adapter.submitList(posts)
            }
        }
    }
