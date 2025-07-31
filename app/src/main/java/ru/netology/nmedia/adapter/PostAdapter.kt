package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ShortNumberFormatter
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post

typealias OnItemLikeListener = (post: Post) -> Unit
typealias OnItemShareListener = (post: Post) -> Unit

class PostAdapter(
    private val onItemLikeListener: OnItemLikeListener,
    private val onItemShareListener: OnItemShareListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onItemLikeListener, onItemShareListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onItemLikeListener: OnItemLikeListener,
    private val onItemShareListener: OnItemShareListener,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
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
            likes.setOnClickListener {
                onItemLikeListener(post)
            }

            reposts.setOnClickListener {
                onItemShareListener(post)
            }
        }
    }
}

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {

    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

}