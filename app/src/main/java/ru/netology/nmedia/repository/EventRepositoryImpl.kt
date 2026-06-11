package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.EventApiService
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.EventDao
import ru.netology.nmedia.dao.EventRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.entity.EventEntity
import ru.netology.nmedia.entity.PostEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventApiService: EventApiService,
    private val postApiService: PostApiService,
    private val eventRemoteKeyDao: EventRemoteKeyDao,
    private val appDb: AppDb,
    private val appAuth: AppAuth,
) : EventRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagingData(): Flow<PagingData<Event>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { eventDao.getPagingSource() },
        remoteMediator = EventRemoteMediator(
            apiService = eventApiService,
            eventDao = eventDao,
            eventRemoteKeyDao = eventRemoteKeyDao,
            appDb = appDb,
        ),
    ).flow.map { paging -> paging.map { it.toDto() } }

    override suspend fun getById(id: Long): Event? {
        return try {
            val response = eventApiService.getById(id)
            if (response.isSuccessful) {
                response.body()?.also { eventDao.insert(listOf(EventEntity.fromDto(it))) }
            } else {
                eventDao.getByIdSync(id)?.toDto()
            }
        } catch (_: Exception) {
            eventDao.getByIdSync(id)?.toDto()
        }
    }

    override suspend fun likeById(id: Long): Event = toggleLike(id)

    override suspend fun unlikeById(id: Long): Event = toggleLike(id)

    private suspend fun toggleLike(id: Long): Event {
        val userId = appAuth.authState.value?.resolvedId()
            ?: throw RuntimeException("Not authenticated")
        val entity = eventDao.getByIdSync(id) ?: throw RuntimeException("Event not found")
        val optimistic = applyLikeToggle(entity, userId)
        eventDao.insert(listOf(optimistic))
        return try {
            if (entity.likedByMe) {
                eventApiService.unlikeById(id)
            } else {
                eventApiService.likeById(id)
            }
            refreshFromApi(id)
        } catch (e: Exception) {
            eventDao.insert(listOf(entity))
            entity.toDto()
        }
    }

    override suspend fun toggleParticipation(id: Long): Event {
        val userId = appAuth.authState.value?.resolvedId()
            ?: throw RuntimeException("Not authenticated")
        val entity = eventDao.getByIdSync(id) ?: throw RuntimeException("Event not found")
        val optimistic = applyParticipationToggle(entity, userId)
        eventDao.insert(listOf(optimistic))
        return try {
            if (entity.participatedByMe) {
                eventApiService.unparticipate(id)
            } else {
                eventApiService.participate(id)
            }
            refreshFromApi(id)
        } catch (e: Exception) {
            eventDao.insert(listOf(entity))
            entity.toDto()
        }
    }

    private fun applyLikeToggle(entity: EventEntity, userId: Long): EventEntity {
        val ids = entity.likeOwnerIds.toMutableList()
        val liked = !entity.likedByMe
        if (liked) {
            if (!ids.contains(userId)) ids.add(userId)
        } else {
            ids.remove(userId)
        }
        return entity.copy(likedByMe = liked, likeOwnerIds = ids)
    }

    private fun applyParticipationToggle(entity: EventEntity, userId: Long): EventEntity {
        val ids = entity.participantsIds.toMutableList()
        val participated = !entity.participatedByMe
        if (participated) {
            if (!ids.contains(userId)) ids.add(userId)
        } else {
            ids.remove(userId)
        }
        return entity.copy(participatedByMe = participated, participantsIds = ids)
    }

    override suspend fun removeById(id: Long) {
        eventDao.removeById(id)
        try {
            eventApiService.removeById(id)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun save(
        event: Event,
        attachmentFile: File?,
        attachmentType: AttachmentType?,
    ): Event {
        val media = attachmentFile?.let { upload(it) }
        val eventWithAttachment = if (media != null && attachmentType != null) {
            event.copy(attachment = Attachment(url = media.url, type = attachmentType))
        } else {
            event
        }

        val response = eventApiService.save(eventWithAttachment)
        if (!response.isSuccessful) {
            throw RuntimeException(response.message())
        }
        val saved = response.body() ?: throw RuntimeException("Event not saved")
        val result = saved.copy(
            attachment = saved.attachment ?: eventWithAttachment.attachment,
            authorAvatar = saved.authorAvatar ?: eventWithAttachment.authorAvatar,
        )
        eventDao.insert(listOf(EventEntity.fromDto(result)))
        return result
    }

    override suspend fun edit(eventId: Long, content: String) {
        eventDao.edit(eventId, content)
        try {
            val event = eventApiService.getById(eventId).body()
                ?: throw RuntimeException("Event not found")
            eventApiService.save(event.copy(content = content))
        } catch (_: Exception) {
        }
    }

    private suspend fun refreshFromApi(id: Long): Event {
        val updated = eventApiService.getById(id).body() ?: throw RuntimeException("Event not found")
        eventDao.insert(listOf(EventEntity.fromDto(updated)))
        return updated
    }

    private suspend fun upload(file: File): Media =
        postApiService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(),
            ),
        )
}
