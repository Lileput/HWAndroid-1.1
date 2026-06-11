package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Event
import java.io.File

interface EventRepository {
    fun getPagingData(): Flow<PagingData<Event>>

    suspend fun getById(id: Long): Event?

    suspend fun likeById(id: Long): Event

    suspend fun unlikeById(id: Long): Event

    suspend fun toggleParticipation(id: Long): Event

    suspend fun removeById(id: Long)

    suspend fun save(
        event: Event,
        attachmentFile: File?,
        attachmentType: AttachmentType?,
    ): Event

    suspend fun edit(eventId: Long, content: String)
}
