package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

    interface PostRepository {
        val data: Flow<List<Post>>
        fun getNewer(id: Long): Flow<Int>
        suspend fun getAll()
        suspend fun likeById(id: Long) : Post
        suspend fun unlikeById(id: Long) : Post
        suspend fun removeById(id: Long)
        suspend fun reposts(id: Long)
        suspend fun save(post: Post) : Post
        suspend fun edit(postId : Long, content : String)
    }