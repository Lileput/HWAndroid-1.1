package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface WallRepository {
    fun getPagingData(authorId: Long): Flow<PagingData<Post>>
}
