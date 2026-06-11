package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.WallRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: PostApiService,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    private val appDb: AppDb,
) : WallRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(authorId: Long): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { postDao.getWallPagingSource(authorId) },
        remoteMediator = WallRemoteMediator(
            authorId = authorId,
            apiService = apiService,
            postDao = postDao,
            wallRemoteKeyDao = wallRemoteKeyDao,
            appDb = appDb,
        ),
    ).flow.map { pagingData -> pagingData.map(PostEntity::toDto) }
}
