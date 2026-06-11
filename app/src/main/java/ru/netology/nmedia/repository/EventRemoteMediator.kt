package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.EventApiService
import ru.netology.nmedia.dao.EventDao
import ru.netology.nmedia.dao.EventRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.EventEntity
import ru.netology.nmedia.entity.EventRemoteKeyEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class EventRemoteMediator(
    private val apiService: EventApiService,
    private val eventDao: EventDao,
    private val eventRemoteKeyDao: EventRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, EventEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EventEntity>,
    ): MediatorResult {
        try {
            if (loadType == LoadType.PREPEND) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val result = when (loadType) {
                LoadType.APPEND -> {
                    val beforeKey = eventRemoteKeyDao.getBefore()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    apiService.getBefore(beforeKey, state.config.pageSize)
                }

                LoadType.REFRESH -> {
                    val afterKey = eventRemoteKeyDao.getAfter()
                    if (afterKey == null) {
                        apiService.getLatest(state.config.pageSize)
                    } else {
                        apiService.getAfter(afterKey, state.config.pageSize)
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
                            val afterKey = eventRemoteKeyDao.getAfter()
                            val isFirstLoad = afterKey == null

                            if (isFirstLoad) {
                                val newMaxId = data.maxOfOrNull { it.id } ?: return@withTransaction
                                val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                                eventRemoteKeyDao.insert(
                                    EventRemoteKeyEntity(EventRemoteKeyEntity.KeyType.AFTER, newMaxId),
                                )
                                eventRemoteKeyDao.insert(
                                    EventRemoteKeyEntity(EventRemoteKeyEntity.KeyType.BEFORE, newMinId),
                                )
                            } else {
                                val newMaxId = data.maxOfOrNull { it.id } ?: return@withTransaction
                                eventRemoteKeyDao.insert(
                                    EventRemoteKeyEntity(EventRemoteKeyEntity.KeyType.AFTER, newMaxId),
                                )
                            }
                            eventDao.insert(data.map { EventEntity.fromDto(it) })
                        }
                    }

                    LoadType.APPEND -> {
                        if (data.isNotEmpty()) {
                            val newMinId = data.minOfOrNull { it.id } ?: return@withTransaction
                            eventRemoteKeyDao.insert(
                                EventRemoteKeyEntity(EventRemoteKeyEntity.KeyType.BEFORE, newMinId),
                            )
                            eventDao.insert(data.map { EventEntity.fromDto(it) })
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
