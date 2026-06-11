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
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ShortNumberFormatter
import ru.netology.nmedia.databinding.CardEventBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.util.Formatter
import ru.netology.nmedia.util.ImageLoader
import ru.netology.nmedia.util.LinkUtils

interface OnEventInteractionListener {
    fun like(event: Event)
    fun remove(event: Event)
    fun edit(event: Event)
    fun share(event: Event)
    fun participate(event: Event)
    fun onPlayVideo(videoUrl: String)
    fun showDeleteConfirmation(event: Event)
    fun onItemClick(event: Event)
    fun onImageClick(imageUrl: String)
}

class EventAdapter(
    private val onInteractionListener: OnEventInteractionListener,
) : PagingDataAdapter<Event, EventViewHolder>(EventDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = CardEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position) ?: return
        holder.bind(event)
    }

    override fun onViewRecycled(holder: EventViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImages()
    }
}

class EventViewHolder(
    private val binding: CardEventBinding,
    private val onInteractionListener: OnEventInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(event: Event, openOnClick: Boolean = true) {
        binding.apply {
            author.text = event.author
            published.text = Formatter.formatDateTime(event.published)
            eventType.text = when (event.type) {
                EventType.ONLINE -> root.context.getString(R.string.event_type_online)
                EventType.OFFLINE -> root.context.getString(R.string.event_type_offline)
            }
            eventDatetime.text = Formatter.formatDateTime(event.datetime)
            LinkUtils.bindTextWithLinks(content, event.content)

            likes.text = ShortNumberFormatter.format(event.likesCount)
            likes.isChecked = event.likedByMe
            participants.text = ShortNumberFormatter.format(event.participantsCount)
            participants.isChecked = event.participatedByMe

            ImageLoader.loadAvatar(avatar, event.authorAvatar, event.id)

            val separateLink = event.link?.takeIf { url ->
                url.isNotBlank() && !event.content.contains(url, ignoreCase = true)
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
                root.setOnClickListener { onInteractionListener.onItemClick(event) }
            } else {
                root.setOnClickListener(null)
                root.isClickable = false
            }

            likes.setOnClickListener {
                val delta = if (event.likedByMe) -1 else 1
                likes.text = ShortNumberFormatter.format((event.likesCount + delta).coerceAtLeast(0))
                onInteractionListener.like(event)
            }
            share.setOnClickListener { onInteractionListener.share(event) }
            participants.setOnClickListener {
                val delta = if (event.participatedByMe) -1 else 1
                participants.text = ShortNumberFormatter.format(
                    (event.participantsCount + delta).coerceAtLeast(0),
                )
                participants.isChecked = !event.participatedByMe
                onInteractionListener.participate(event)
            }

            menu.isVisible = event.ownedByMe
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                AlertDialog.Builder(it.context)
                                    .setMessage(R.string.delete_event_confirmation)
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        onInteractionListener.remove(event)
                                    }
                                    .setNegativeButton(R.string.no, null)
                                    .show()
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.edit(event)
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

            when (event.attachment?.type) {
                AttachmentType.IMAGE -> {
                    attachmentContainer.isVisible = true
                    ImageLoader.loadAttachmentImage(attachmentImage, event.attachment.url, event.id)
                    val openImage = View.OnClickListener {
                        onInteractionListener.onImageClick(event.attachment.url)
                    }
                    attachmentContainer.setOnClickListener(openImage)
                    attachmentImage.setOnClickListener(openImage)
                }
                AttachmentType.VIDEO -> {
                    videoContainer.isVisible = true
                    ImageLoader.loadVideoPreview(videoPreview, event.attachment.url, event.id)
                    val play = View.OnClickListener {
                        ImageLoader.clear(videoPreview)
                        onInteractionListener.onPlayVideo(event.attachment.url)
                    }
                    videoContainer.setOnClickListener(play)
                    playButton.setOnClickListener(play)
                }
                AttachmentType.AUDIO -> {
                    audioPlay.isVisible = true
                    audioPlay.setOnClickListener {
                        onInteractionListener.onPlayVideo(event.attachment.url)
                    }
                }
                null -> Unit
            }
        }
    }

    fun clearImages() {
        ImageLoader.clear(binding.avatar)
        ImageLoader.clear(binding.attachmentImage)
        ImageLoader.clear(binding.videoPreview)
    }
}

object EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
}
