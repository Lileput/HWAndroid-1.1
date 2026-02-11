package ru.netology.nmedia.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ShortNumberFormatter
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.ImageLoader

interface OnInteractionListener {
    fun like(post: Post)
    fun remove(post: Post)
    fun edit(post: Post)
    fun repost(post: Post)
    fun onPlayVideo(videoUrl: String)
    fun showDeleteConfirmation(post: Post)
    fun onItemClick(post: Post)
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener,
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = formatDate(post.published)
            content.text = post.content
            countEyes.text = ShortNumberFormatter.format(post.views)
            likes.text = ShortNumberFormatter.format(post.likes)
            reposts.text = ShortNumberFormatter.format(post.shares)

            likes.isChecked = post.likedByMe
            reposts.isChecked = post.sharedByMe
            ImageLoader.loadAvatar(avatar, post.authorAvatar)

            root.setOnClickListener {
                onInteractionListener.onItemClick(post)
            }

            likes.setOnClickListener {
                onInteractionListener.like(post)
            }

            reposts.setOnClickListener {
                onInteractionListener.repost(post)
            }
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                AlertDialog.Builder(it.context)
                                    .setMessage(R.string.delete_post_confirmation)
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        onInteractionListener.remove(post)
                                    }
                                    .setNegativeButton(R.string.no, null)
                                    .show()
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.edit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            if (post.attachment != null && post.attachment.type == "IMAGE" && !post.attachment.url.isNullOrBlank()) {
                attachmentContainer.visibility = View.VISIBLE
                attachmentImage.visibility = View.VISIBLE
                ImageLoader.loadAttachmentImage(attachmentImage, post.attachment.url)
            } else {
                attachmentContainer.visibility = View.GONE
                attachmentImage.visibility = View.GONE
            }

            if (post.video != null) {
                videoContainer.visibility = View.VISIBLE
                videoContainer.setOnClickListener { onInteractionListener.onPlayVideo(post.video) }
                playButton.setOnClickListener { onInteractionListener.onPlayVideo(post.video) }
            } else {
                videoContainer.visibility = View.GONE
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestamp

        return when {
            diff < 60 -> "Только что"
            diff < 3600 -> "${diff / 60} мин."
            diff < 86400 -> "${diff / 3600} ч."
            diff < 2592000 -> "${diff / 86400} дн."
            else -> "Более месяца назад"
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