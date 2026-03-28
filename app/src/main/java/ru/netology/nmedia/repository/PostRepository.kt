package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
        fun getPagingData(): Flow<PagingData<Post>>
        suspend fun likeById(id: Long) : Post
        suspend fun unlikeById(id: Long) : Post
        suspend fun removeById(id: Long)
        suspend fun reposts(id: Long)
        suspend fun save(post: Post, image: File?) : Post
        suspend fun edit(postId : Long, content : String)
        suspend fun getNewPostsCount(): Flow<Int>
        suspend fun markAllPostsAsRead()
        suspend fun showNewPosts()
    }