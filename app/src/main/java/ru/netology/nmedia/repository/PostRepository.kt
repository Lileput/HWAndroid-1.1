package ru.netology.nmedia.repository


import ru.netology.nmedia.dto.Post

    interface PostRepository {
        fun get(): List<Post>
        fun likeById(id: Long, callback: RepositoryCallback<Unit>)
        fun unlikeById(id: Long, callback: RepositoryCallback<Unit>)
        fun removeById(id: Long)
        fun reposts(id: Long)
        fun save(post: Post)
        fun edit(postId : Long, content : String)
        fun getAllAsync(callback: GetAllCallback)

        interface GetAllCallback {
            fun onSuccess(posts: List<Post>)
            fun onError(e: Throwable)
        }

        interface RepositoryCallback<T> {
            fun onSuccess(data: T)
            fun onError(e: Exception)
        }
    }