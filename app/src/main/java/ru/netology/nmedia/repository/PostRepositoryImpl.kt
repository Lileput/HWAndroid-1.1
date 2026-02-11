package ru.netology.nmedia.repository


import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.AppError

class PostRepositoryImpl(
    private val postDao: PostDao,
) : PostRepository {
    override val data: Flow<List<Post>> = postDao.getAll().map { it.map {it.toDto()} }

    override suspend fun getAll() {
        try {
            val response = PostApi.service.getAll()

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }

            val posts = response.body() ?: throw RuntimeException("Response body is empty")

            val entities = posts.map(PostEntity.Companion::fromDto)
            postDao.clearAndInsert(entities)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun likeById(id: Long): Post {
        try {
            postDao.likeById(id)
            val localPost = postDao.getByIdSync(id)?.toDto() ?: throw RuntimeException("Post not found")

            try {
                if (localPost.likedByMe) {
                    PostApi.service.likeById(id)
                } else {
                    PostApi.service.unlikeById(id)
                }

                val updatedPost = PostApi.service.getById(id).body() ?: throw RuntimeException("Post not found")

                postDao.updatePost(PostEntity.fromDto(updatedPost))

                return updatedPost
            } catch (e: Exception) {
                return localPost
            }

        } catch (e: Exception) {
            throw RuntimeException("Failed to like post: ${e.message}")
        }
    }

    override suspend fun unlikeById(id: Long): Post {
        return likeById(id)
    }

    override suspend fun removeById(id: Long) {
        try {
            postDao.removeById(id)

            try {
                PostApi.service.removeById(id)
            } catch (e: Exception) {
                throw e
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to remove post: ${e.message}")
        }
    }

    override suspend fun reposts(id: Long) {
        try {
            postDao.reposts(id)

            try {
                PostApi.service.reposts(id)
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to repost: ${e.message}")
        }
    }

    override suspend fun save(post: Post): Post {
        val tempId = -System.currentTimeMillis()
        val localPost = post.copy(id = tempId)

        val entity = PostEntity.fromDto(localPost).copy(syncStatus = PostEntity.SyncStatus.PENDING)
        postDao.insert(entity)

        return try {
            val response = PostApi.service.save(post)

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }

            val savedPost = response.body() ?: throw RuntimeException("Post not saved")

            postDao.updatePost(PostEntity.fromDto(savedPost).copy(syncStatus = PostEntity.SyncStatus.SYNCED))

            postDao.removeById(tempId)

            savedPost
        } catch (e: Exception) {
            postDao.updateSyncStatus(tempId, PostEntity.SyncStatus.FAILED)
            throw e
        }
    }


    override suspend fun edit(postId: Long, content: String) {
        try {
            postDao.edit(postId, content)

            try {
                val post = PostApi.service.getById(postId).body() ?: throw RuntimeException("Post not found")

                val updatedPost = post.copy(content = content)
                PostApi.service.save(updatedPost)
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to edit post: ${e.message}")
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while(true) {
            delay(10_000L)
            val response = PostApi.service.getNewer(id)

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }

            val body = response.body() ?: throw RuntimeException("Response body is empty")
            val entities = body.map { post ->
                PostEntity.fromDto(post)
            }

            postDao.insert(entities)
            emit(body.size)
        }
    }.catch { e -> throw AppError.from(e) }
}