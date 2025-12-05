package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String = "Default Author",
    val published: Long = 0,
    val content: String,
    val likes: Int = 0,
    val share: Int = 0,
    val views: Int = 0,
    val likedByMe: Int = 0,
    val shareByMe: Int =0,
    val video: String? = null,
) {
    fun toDto(): Post = Post(
        id = id,
        author = author,
        published = published,
        content = content,
        likes = likes,
        share = share,
        views = views,
        likedByMe = likedByMe != 0,
        shareByMe = shareByMe != 0
    )
}

fun Post.toEntity(): PostEntity = PostEntity(
    id = id,
    author = author,
    published = published,
    content = content,
    likes = likes,
    share = share,
    views = views,
    likedByMe = if (likedByMe) 1 else 0,
    shareByMe = if (shareByMe) 1 else 0
)