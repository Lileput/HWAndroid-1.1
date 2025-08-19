package ru.netology.nmedia.adapter

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ShortNumberFormatter
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post

interface onInteractionListener {
    fun like(post: Post)
    fun remove(post: Post)
    fun repost(post: Post)
    fun edit(post: Post)
}

class PostAdapter(
    private val onInteractionListener: onInteractionListener,
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
    private val OnInteractionListener: onInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            countEyes.text = ShortNumberFormatter.format(post.views)
            likes.text = ShortNumberFormatter.format(post.likes)
            reposts.text = ShortNumberFormatter.format(post.share)

            likes.isChecked = post.likeByMe
            reposts.isChecked = post.shareByMe

            likes.setOnClickListener {
                OnInteractionListener.like(post)
            }

            reposts.setOnClickListener {
                OnInteractionListener.repost(post)
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
                                        OnInteractionListener.remove(post)
                                    }
                                    .setNegativeButton(R.string.no, null)
                                    .show()
                                true
                            }

                            R.id.edit -> {
                                OnInteractionListener.edit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
            if (post.video != null) {
                videoContainer.visibility = View.VISIBLE
                playButton.contentDescription = "Воспроизвести видео"
                videoContainer.setOnClickListener { openVideo(post.video, it.context) }
                playButton.setOnClickListener { openVideo(post.video, it.context) }
            } else {
                videoContainer.visibility = View.GONE
            }
        }
    }
    private fun openVideo(videoUrl: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_video_app, Toast.LENGTH_SHORT).show()
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