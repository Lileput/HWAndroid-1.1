package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val authorId: Long,
    val link: String? = null,
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
        published = epochToPublished(published),
        link = link,
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
        fun publishedToEpoch(value: String): Long {
            if (value.isBlank()) return 0L
            return try {
                if (value.all { it.isDigit() }) value.toLong()
                else {
                    parseIsoDate(value)?.time?.div(1000) ?: 0L
                }
            } catch (_: Exception) {
                0L
            }
        }

        fun epochToPublished(epoch: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(epoch * 1000))
        }

        private fun parseIsoDate(value: String): Date? {
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val parsed = sdf.parse(value)
                    if (parsed != null) return parsed
                } catch (_: Exception) {
                }
            }
            return null
        }

        fun fromDto(dto: Post) = PostEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            authorId = dto.authorId,
            link = dto.link,
            content = dto.content,
            published = publishedToEpoch(dto.published),
            likes = dto.likesCount,
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
