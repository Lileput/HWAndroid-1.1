package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: PostApiService,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : PostRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { postDao.getPagingSource() },
        remoteMediator = PostRemoteMediator(
            apiService = apiService,
            postDao = postDao,
            postRemoteKeyDao = postRemoteKeyDao,
            appDb = appDb,
            )
    ).flow
        .map { it.map (PostEntity::toDto) }

    override suspend fun likeById(id: Long): Post {
        try {
            postDao.likeById(id)
            val localPost =
                postDao.getByIdSync(id)?.toDto() ?: throw RuntimeException("Post not found")

            try {
                if (localPost.likedByMe) {
                    apiService.likeById(id)
                } else {
                    apiService.unlikeById(id)
                }

                val updatedPost =
                    apiService.getById(id).body() ?: throw RuntimeException("Post not found")

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
                apiService.removeById(id)
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
                apiService.reposts(id)
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to repost: ${e.message}")
        }
    }

    override suspend fun save(post: Post, image: File?): Post {
        val tempId = -System.currentTimeMillis()
        val localPost = post.copy(id = tempId)

        val entity = PostEntity.fromDto(localPost).copy(syncStatus = PostEntity.SyncStatus.PENDING)
        postDao.insert(entity)

        return try {
            val media = image?.let {
                upload(it)
            }

            val postWithAttachment = media?.let {
                post.copy(attachment = Attachment(url = it.id, AttachmentType.IMAGE))
            } ?: post

            val response = apiService.save(postWithAttachment)

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }

            val savedPost = response.body() ?: throw RuntimeException("Post not saved")

            postDao.updatePost(
                PostEntity.fromDto(savedPost).copy(syncStatus = PostEntity.SyncStatus.SYNCED)
            )

            postDao.removeById(tempId)

            savedPost
        } catch (e: Exception) {
            postDao.updateSyncStatus(tempId, PostEntity.SyncStatus.FAILED)
            throw e
        }
    }

    private suspend fun upload(file: File): Media =
        apiService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(),
            )
        )


    override suspend fun edit(postId: Long, content: String) {
        try {
            postDao.edit(postId, content)

            try {
                val post = apiService.getById(postId).body()
                    ?: throw RuntimeException("Post not found")

                val updatedPost = post.copy(content = content)
                apiService.save(updatedPost)
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to edit post: ${e.message}")
        }
    }

    override suspend fun showNewPosts() {

        postDao.showAllHiddenPosts()

        postDao.markAllAsNotNew()
    }

    override suspend fun getNewPostsCount(): Flow<Int> = flow {
        while (true) {
            emit(postDao.getNewPostsCount())
            delay(1000L)
        }
    }

    override suspend fun markAllPostsAsRead() {
        postDao.markAllAsNotNew()
    }
}