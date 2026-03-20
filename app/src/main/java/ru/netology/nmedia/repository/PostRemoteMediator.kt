package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: PostApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {

            if (loadType == LoadType.PREPEND) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val result = when (loadType) {
                LoadType.APPEND -> {
                    val beforeKey = postRemoteKeyDao.getBefore() ?: return MediatorResult.Success(false)
                    apiService.getBefore(beforeKey, state.config.pageSize)
                }

                LoadType.REFRESH -> {
                    val afterKey = postRemoteKeyDao.getAfter()
                    if (afterKey == null) {
                        apiService.getLatest(state.config.pageSize)
                    } else {
                        apiService.getAfter(afterKey, state.config.pageSize)
                    }
                }

                else -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            if (!result.isSuccessful) {
                throw HttpException(result)
            }

            val data = result.body().orEmpty()

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (data.isNotEmpty()) {
                            val newMaxId = data.maxOfOrNull { it.id } ?: return@withTransaction
                            val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    newMaxId
                                )
                            )
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    newMinId
                                )
                            )
                            postDao.insert(data.map { PostEntity.fromDto(it) })
                        }
                    }

                    LoadType.PREPEND -> {}

                    LoadType.APPEND -> {
                        if (data.isNotEmpty()) {
                            val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    newMinId
                                )
                            )
                            postDao.insert(data.map { PostEntity.fromDto(it) })
                        }
                    }
                }
            }
            return MediatorResult.Success(data.isEmpty())
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }
}