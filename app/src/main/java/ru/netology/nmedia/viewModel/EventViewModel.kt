package ru.netology.nmedia.viewModel

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.EventType
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.model.NewPostAttachment
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.EventRepository
import ru.netology.nmedia.repository.UserRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    private val appAuth: AppAuth,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private var cachedCurrentUser: User? = null

    val data: Flow<PagingData<Event>> = appAuth.authState.flatMapLatest { token ->
        repository.getPagingData()
            .map { paging ->
                paging.map { event ->
                    event.copy(ownedByMe = event.authorId == token?.resolvedId())
                }
            }
    }.flowOn(Dispatchers.Default)

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit> = _eventCreated

    private val _errorMessage = SingleLiveEvent<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    private val _mediaAttachment = MutableLiveData<NewPostAttachment?>(null)
    val mediaAttachment: LiveData<NewPostAttachment?> = _mediaAttachment

    private val _coords = MutableLiveData<Coordinates?>(null)
    val coords: LiveData<Coordinates?> = _coords

    private val _speakerIds = MutableLiveData<Set<Long>>(emptySet())
    val speakerIds: LiveData<Set<Long>> = _speakerIds

    private val _eventDatetimeEpoch = MutableLiveData<Long?>(null)
    val eventDatetimeEpoch: LiveData<Long?> = _eventDatetimeEpoch

    private val _eventType = MutableLiveData(EventType.ONLINE)
    val eventType: LiveData<EventType> = _eventType

    private val _shouldAuthenticate = SingleLiveEvent<Unit>()
    val shouldAuthenticate: LiveData<Unit> = _shouldAuthenticate

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            appAuth.authState.collect { token ->
                val userId = token?.resolvedId() ?: 0L
                cachedCurrentUser = if (userId != 0L) {
                    runCatching { userRepository.getById(userId) }.getOrNull()
                } else {
                    null
                }
            }
        }
    }

    fun setEventDetails(datetimeEpoch: Long, type: EventType) {
        _eventDatetimeEpoch.value = datetimeEpoch
        _eventType.value = type
    }

    fun setCoords(coords: Coordinates?) {
        _coords.value = coords
    }

    fun setSpeakerIds(ids: Set<Long>) {
        _speakerIds.value = ids
    }

    fun changePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
        _mediaAttachment.value = null
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun setMediaAttachment(attachment: NewPostAttachment) {
        _mediaAttachment.value = attachment
        _photo.value = null
    }

    fun removeMediaAttachment() {
        _mediaAttachment.value = null
    }

    fun clearNewEventState() {
        _photo.value = null
        _mediaAttachment.value = null
        _coords.value = null
        _speakerIds.value = emptySet()
        _eventDatetimeEpoch.value = null
        _eventType.value = EventType.ONLINE
    }

    fun like(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_like)
            }
        }
    }

    fun unlike(id: Long) {
        viewModelScope.launch {
            try {
                repository.unlikeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_unlike)
            }
        }
    }

    fun toggleParticipation(id: Long) {
        viewModelScope.launch {
            try {
                repository.toggleParticipation(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_participate)
            }
        }
    }

    fun toggleParticipationWithCheck(id: Long) {
        if (appAuth.authState.value == null) {
            _shouldAuthenticate.value = Unit
            return
        }
        toggleParticipation(id)
    }

    fun likeWithCheck(id: Long) {
        if (appAuth.authState.value == null) {
            _shouldAuthenticate.value = Unit
            return
        }
        like(id)
    }

    fun unlikeWithCheck(id: Long) {
        if (appAuth.authState.value == null) {
            _shouldAuthenticate.value = Unit
            return
        }
        unlike(id)
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_delete_event)
            }
        }
    }

    fun saveWithCheck(content: String) {
        if (appAuth.authState.value == null) {
            _shouldAuthenticate.value = Unit
            return
        }
        save(content)
    }

    fun save(text: String) {
        viewModelScope.launch {
            try {
                val content = text.trim()
                if (content.isEmpty()) return@launch

                val datetimeEpoch = _eventDatetimeEpoch.value
                if (datetimeEpoch == null) {
                    _errorMessage.value = appContext.getString(R.string.event_datetime_required)
                    return@launch
                }

                val user = cachedCurrentUser
                val photo = _photo.value
                val media = _mediaAttachment.value
                val attachmentFile = photo?.file ?: media?.file
                val attachmentType = when {
                    photo != null -> AttachmentType.IMAGE
                    media != null -> media.type
                    else -> null
                }

                val eventToSave = Event(
                    id = 0L,
                    authorId = user?.id ?: appAuth.authState.value?.resolvedId() ?: 0L,
                    author = user?.name ?: "Me",
                    authorAvatar = user?.avatar ?: appAuth.authState.value?.avatar,
                    content = content,
                    datetime = PostEntity.epochToPublished(datetimeEpoch),
                    published = PostEntity.epochToPublished(System.currentTimeMillis() / 1000),
                    type = _eventType.value ?: EventType.ONLINE,
                    coords = _coords.value,
                    speakerIds = _speakerIds.value.orEmpty().toList(),
                    participantsIds = emptyList(),
                    likedByMe = false,
                    participatedByMe = false,
                    likeOwnerIds = emptyList(),
                    attachment = null,
                    link = null,
                )

                repository.save(eventToSave, attachmentFile, attachmentType)
                _eventCreated.value = Unit
                clearNewEventState()
            } catch (e: Exception) {
                handleError(e, R.string.error_save_event)
            }
        }
    }

    fun edit(eventId: Long, newText: String) {
        viewModelScope.launch {
            try {
                repository.edit(eventId, newText.trim())
            } catch (e: Exception) {
                handleError(e, R.string.error_edit_event)
            }
        }
    }

    fun showAttachmentTooLargeError() {
        _errorMessage.value = appContext.getString(R.string.error_attachment_too_large)
    }

    private fun handleError(e: Throwable, @StringRes defaultMessageRes: Int) {
        val defaultMessage = appContext.getString(defaultMessageRes)
        val errorMessage = when (e) {
            is HttpException -> appContext.getString(R.string.error_with_code, e.code())
            is IOException -> appContext.getString(R.string.network_error)
            else -> e.message ?: defaultMessage
        }
        _errorMessage.value = errorMessage
    }
}
