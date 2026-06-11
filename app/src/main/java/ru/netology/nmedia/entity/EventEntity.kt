package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.dto.PostUserPreview

@Entity
data class EventEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorId: Long,
    val authorJob: String? = null,
    val link: String? = null,
    val content: String,
    val published: Long,
    val datetime: Long,
    val type: EventType,
    val coords: Coordinates? = null,
    val attachment: Attachment? = null,
    val likedByMe: Boolean = false,
    val participatedByMe: Boolean = false,
    val likeOwnerIds: List<Long> = emptyList(),
    val speakerIds: List<Long> = emptyList(),
    val participantsIds: List<Long> = emptyList(),
    val users: Map<String, PostUserPreview> = emptyMap(),
    val hidden: Boolean = false,
) {
    fun toDto(): Event = Event(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        authorJob = authorJob,
        content = content,
        datetime = PostEntity.epochToPublished(datetime),
        published = PostEntity.epochToPublished(published),
        coords = coords,
        type = type,
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        speakerIds = speakerIds,
        participantsIds = participantsIds,
        participatedByMe = participatedByMe,
        attachment = attachment,
        link = link,
        users = this.users,
    )

    companion object {
        fun fromDto(dto: Event) = EventEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            authorId = dto.authorId,
            authorJob = dto.authorJob,
            link = dto.link,
            content = dto.content,
            published = PostEntity.publishedToEpoch(dto.published),
            datetime = PostEntity.publishedToEpoch(dto.datetime),
            type = dto.type,
            coords = dto.coords,
            attachment = dto.attachment,
            likedByMe = dto.likedByMe,
            participatedByMe = dto.participatedByMe,
            likeOwnerIds = dto.likeOwnerIds,
            speakerIds = dto.speakerIds,
            participantsIds = dto.participantsIds,
            users = dto.users,
            hidden = false,
        )
    }
}
