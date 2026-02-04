package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: Long,
    val likes: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    val likedByMe: Boolean = false,
    val sharedByMe: Boolean = false,
    val video: String? = null,
    val attachment: Attachment? = null,
    val comments: Int = 0,
    val commentByMe: Boolean = false
)

data class Attachment(
    val url: String,
    val description: String?,
    val type: String
)
