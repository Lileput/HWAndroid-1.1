package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

    interface PostRepository {
        val data: LiveData<List<Post>>
        suspend fun getAll()
        suspend fun likeById(id: Long) : Post
        suspend fun unlikeById(id: Long) : Post
        suspend fun removeById(id: Long)
        suspend fun reposts(id: Long)
        suspend fun save(post: Post) : Post
        suspend fun edit(postId : Long, content : String)
    }