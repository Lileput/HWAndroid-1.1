package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String = "Default Author",
    val published: String = "Now",
    val content: String,
    val likes: Int = 0,
    val share: Int = 0,
    val views: Int = 0,
    val likeByMe: Boolean = false,
    val shareByMe: Boolean = false,
    val video: String? = null,
)
