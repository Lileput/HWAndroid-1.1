package ru.netology.nmedia.adapter

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ShortNumberFormatter
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.util.Formatter
import ru.netology.nmedia.util.ImageLoader
import ru.netology.nmedia.util.LinkUtils

interface OnInteractionListener {
    fun like(post: Post)
    fun remove(post: Post)
    fun edit(post: Post)
    fun repost(post: Post)
    fun onPlayVideo(videoUrl: String)
    fun showDeleteConfirmation(post: Post)
    fun onItemClick(post: Post)
    fun onImageClick(imageUrl: String)
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.forEach {
                (it as? Payload)?.let {
                    holder.bind(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position) ?: return
        holder.bind(post)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImages()
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post, openOnClick: Boolean = true) {
        binding.apply {
            author.text = post.author
            published.text = Formatter.formatPostDateTime(PostEntity.publishedToEpoch(post.published))
            LinkUtils.bindTextWithLinks(content, post.content)
            likes.text = ShortNumberFormatter.format(post.likesCount)
            reposts.text = ShortNumberFormatter.format(post.shares)

            likes.isChecked = post.likedByMe
            reposts.isChecked = post.sharedByMe
            ImageLoader.loadAvatar(avatar, post.authorAvatar, post.id)

            val separateLink = post.link?.takeIf { url ->
                url.isNotBlank() && !post.content.contains(url, ignoreCase = true)
            }
            if (separateLink != null) {
                link.isVisible = true
                link.text = separateLink
                link.setOnClickListener {
                    it.context.startActivity(Intent(Intent.ACTION_VIEW, separateLink.toUri()))
                }
            } else {
                link.isVisible = false
            }

            if (openOnClick) {
                root.setOnClickListener { onInteractionListener.onItemClick(post) }
            } else {
                root.setOnClickListener(null)
                root.isClickable = false
            }

            likes.setOnClickListener {
                val delta = if (post.likedByMe) -1 else 1
                likes.text = ShortNumberFormatter.format((post.likesCount + delta).coerceAtLeast(0))
                onInteractionListener.like(post)
            }

            reposts.setOnClickListener {
                onInteractionListener.repost(post)
            }
            menu.isVisible = post.ownedByMe
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

            attachmentContainer.isVisible = false
            videoContainer.isVisible = false
            audioPlay.isVisible = false
            audioPlay.setOnClickListener(null)
            ImageLoader.clear(attachmentImage)
            ImageLoader.clear(videoPreview)

            val attachment = post.attachment
            when (attachment?.type) {
                AttachmentType.IMAGE -> {
                    attachmentContainer.isVisible = true
                    ImageLoader.loadAttachmentImage(attachmentImage, attachment.url, post.id)
                    val openImage = View.OnClickListener {
                        onInteractionListener.onImageClick(attachment.url)
                    }
                    attachmentContainer.setOnClickListener(openImage)
                    attachmentImage.setOnClickListener(openImage)
                }

                AttachmentType.VIDEO -> {
                    videoContainer.isVisible = true
                    ImageLoader.loadVideoPreview(videoPreview, attachment.url, post.id)
                    val play = View.OnClickListener {
                        ImageLoader.clear(videoPreview)
                        onInteractionListener.onPlayVideo(attachment.url)
                    }
                    videoContainer.setOnClickListener(play)
                    playButton.setOnClickListener(play)
                }

                AttachmentType.AUDIO -> {
                    audioPlay.isVisible = true
                    audioPlay.setOnClickListener {
                        onInteractionListener.onPlayVideo(attachment.url)
                    }
                }

                null -> {
                    if (!post.video.isNullOrBlank()) {
                        videoContainer.isVisible = true
                        ImageLoader.loadVideoPreview(videoPreview, post.video, post.id)
                        val play = View.OnClickListener {
                            ImageLoader.clear(videoPreview)
                            onInteractionListener.onPlayVideo(post.video!!)
                        }
                        videoContainer.setOnClickListener(play)
                        playButton.setOnClickListener(play)
                    }
                }
            }
        }
    }

    fun clearImages() {
        ImageLoader.clear(binding.avatar)
        ImageLoader.clear(binding.attachmentImage)
        ImageLoader.clear(binding.videoPreview)
    }

    fun bind(payload: Payload) {
        payload.likesCount?.let { binding.likes.text = ShortNumberFormatter.format(it) }
        payload.likedByMe?.let { binding.likes.isChecked = it }
        payload.content?.let { LinkUtils.bindTextWithLinks(binding.content, it) }
    }
}

data class Payload(
    val likedByMe: Boolean? = null,
    val likesCount: Int? = null,
    val content: String? = null,
)

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {

    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Post, newItem: Post): Any =
        Payload(
            likedByMe = newItem.likedByMe.takeIf { it != oldItem.likedByMe },
            likesCount = newItem.likesCount.takeIf { it != oldItem.likesCount },
            content = newItem.content.takeIf { it != oldItem.content },
        )
}