package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): List<Post>
    fun likeById(id: Long)
    fun unlikeById(id: Long)
    fun removeById(id: Long)
    fun reposts(id: Long)
    fun save(post: Post) : Post
    fun edit(postId : Long, content : String)
}