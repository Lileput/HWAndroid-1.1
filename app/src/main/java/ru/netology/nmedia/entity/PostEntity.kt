package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likes: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    val likedByMe: Boolean = false,
    val sharedByMe: Boolean = false,
    val video: String? = null,
    val comments: Int = 0,
    val commentByMe: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isNew: Boolean = false,
    val hidden: Boolean = true,
    @Embedded
    val attachment: Attachment?,
) {

    enum class SyncStatus {
        SYNCED,
        PENDING,
        FAILED,
    }

    fun toDto(): Post = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        authorId = authorId,
        content = content,
        published = published,
        likes = likes,
        shares = shares,
        views = views,
        likedByMe = likedByMe,
        sharedByMe = sharedByMe,
        video = video,
        comments = comments,
        commentByMe = commentByMe,
        attachment = attachment,
    )

    companion object {
        fun fromDto(dto: Post) = PostEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            authorId = dto.authorId,
            content = dto.content,
            published = dto.published,
            likes = dto.likes,
            shares = dto.shares,
            views = dto.views,
            likedByMe = dto.likedByMe,
            sharedByMe = dto.sharedByMe,
            video = dto.video,
            comments = dto.comments,
            commentByMe = dto.commentByMe,
            syncStatus = SyncStatus.SYNCED,
            isNew = false,
            hidden = false,
            attachment = dto.attachment
        )
    }
}
