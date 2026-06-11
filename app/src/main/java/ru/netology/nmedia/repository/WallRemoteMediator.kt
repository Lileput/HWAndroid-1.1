package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.WallRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.WallRemoteKeyEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class WallRemoteMediator(
    private val authorId: Long,
    private val apiService: PostApiService,
    private val postDao: PostDao,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>,
    ): MediatorResult {
        try {
            if (loadType == LoadType.PREPEND) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val result = when (loadType) {
                LoadType.APPEND -> {
                    val beforeKey = wallRemoteKeyDao.getBefore(authorId)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    apiService.getWallBefore(authorId, beforeKey, state.config.pageSize)
                }

                LoadType.REFRESH -> {
                    val afterKey = wallRemoteKeyDao.getAfter(authorId)
                    if (afterKey == null) {
                        apiService.getWallLatest(authorId, state.config.pageSize)
                    } else {
                        apiService.getWallAfter(authorId, afterKey, state.config.pageSize)
                    }
                }

                else -> return MediatorResult.Success(endOfPaginationReached = true)
            }

            if (!result.isSuccessful) {
                throw HttpException(result)
            }

            val data = result.body().orEmpty()

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (data.isNotEmpty()) {
                            val afterKey = wallRemoteKeyDao.getAfter(authorId)
                            val isFirstLoad = afterKey == null

                            if (isFirstLoad) {
                                val newMaxId = data.maxOfOrNull { it.id } ?: return@withTransaction
                                val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                                wallRemoteKeyDao.insert(
                                    WallRemoteKeyEntity(
                                        authorId,
                                        WallRemoteKeyEntity.KeyType.AFTER,
                                        newMaxId,
                                    ),
                                )
                                wallRemoteKeyDao.insert(
                                    WallRemoteKeyEntity(
                                        authorId,
                                        WallRemoteKeyEntity.KeyType.BEFORE,
                                        newMinId,
                                    ),
                                )
                            } else {
                                val newMaxId = data.maxOfOrNull { it.id } ?: return@withTransaction
                                wallRemoteKeyDao.insert(
                                    WallRemoteKeyEntity(
                                        authorId,
                                        WallRemoteKeyEntity.KeyType.AFTER,
                                        newMaxId,
                                    ),
                                )
                            }
                            postDao.insert(data.map { PostEntity.fromDto(it) })
                        }
                    }

                    LoadType.APPEND -> {
                        if (data.isNotEmpty()) {
                            val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                            wallRemoteKeyDao.insert(
                                WallRemoteKeyEntity(
                                    authorId,
                                    WallRemoteKeyEntity.KeyType.BEFORE,
                                    newMinId,
                                ),
                            )
                            postDao.insert(data.map { PostEntity.fromDto(it) })
                        }
                    }

                    LoadType.PREPEND -> Unit
                }
            }

            return MediatorResult.Success(endOfPaginationReached = data.isEmpty())
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}
