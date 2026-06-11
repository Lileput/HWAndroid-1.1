package ru.netology.nmedia.viewModel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.model.NewPostAttachment
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.UserRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val userRepository: UserRepository,
    private val appAuth: AppAuth,
    private val prefs: SharedPreferences,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private var cachedCurrentUser: User? = null

    private val draftKey = "post_draft"

    val data: Flow<PagingData<Post>> = appAuth.authState.flatMapLatest { token ->
        repository.getPagingData()
            .map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == token?.resolvedId())
                }
            }
    }
        .flowOn(Dispatchers.Default)

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState>
        get() = _state

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _errorMessage = SingleLiveEvent<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    private val _mediaAttachment = MutableLiveData<NewPostAttachment?>(null)
    val mediaAttachment: LiveData<NewPostAttachment?> = _mediaAttachment

    private val _coords = MutableLiveData<Coordinates?>(null)
    val coords: LiveData<Coordinates?> = _coords

    private val _mentionIds = MutableLiveData<Set<Long>>(emptySet())
    val mentionIds: LiveData<Set<Long>> = _mentionIds

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> = _currentUser

    private val _shouldAuthenticate = SingleLiveEvent<Unit>()
    val shouldAuthenticate: LiveData<Unit> get() = _shouldAuthenticate

    private val _shouldConfirmLogout = SingleLiveEvent<Unit>()
    val shouldConfirmLogout: LiveData<Unit> get() = _shouldConfirmLogout

    init {
        observeNewPosts()
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            appAuth.authState.collect { token ->
                val userId = token?.resolvedId() ?: 0L
                cachedCurrentUser = if (userId != 0L) {
                    val fromApi = runCatching { userRepository.getById(userId) }.getOrNull()
                    when {
                        fromApi == null -> null
                        fromApi.avatar.isNullOrBlank() && !token?.avatar.isNullOrBlank() ->
                            fromApi.copy(avatar = token.avatar)
                        else -> fromApi
                    }
                } else {
                    null
                }
                _currentUser.postValue(cachedCurrentUser)
            }
        }
    }

    private fun observeNewPosts() {
        viewModelScope.launch {
            repository.getNewPostsCount().collect { count ->
                _state.value = _state.value?.copy(newPostsCount = count)
            }
        }
    }

    fun markAllPostsAsRead() {
        viewModelScope.launch {
            repository.markAllPostsAsRead()
        }
    }

    fun showNewPosts() {
        viewModelScope.launch {
            repository.showNewPosts()
        }
    }

    fun saveDraft(content: String) {
        prefs.edit().putString(draftKey, content).apply()
    }

    fun getDraft(): String? = prefs.getString(draftKey, null)

    fun clearDraft() {
        prefs.edit().remove(draftKey).apply()
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

    fun reposts(id: Long) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.reposts(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_repost)
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.removeById(id)
                println("DEBUG: Post $id deleted successfully")
            } catch (e: Exception) {
                handleError(e, R.string.error_delete_post)
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    fun save(text: String) {
        viewModelScope.launch {
            try {
                val content = text.trim()
                if (content.isNotEmpty()) {
                    _state.value = FeedModelState(loading = true)

                    val user = cachedCurrentUser
                    val photo = _photo.value
                    val media = _mediaAttachment.value
                    val attachmentFile = photo?.file ?: media?.file
                    val attachmentType = when {
                        photo != null -> AttachmentType.IMAGE
                        media != null -> media.type
                        else -> null
                    }

                    val postToSave = Post(
                        id = 0L,
                        author = user?.name ?: "Me",
                        authorAvatar = user?.avatar
                            ?: appAuth.authState.value?.avatar,
                        authorId = user?.id ?: appAuth.authState.value?.resolvedId() ?: 0L,
                        content = content,
                        published = PostEntity.epochToPublished(System.currentTimeMillis() / 1000),
                        coords = _coords.value,
                        mentionIds = _mentionIds.value.orEmpty().toList(),
                        mentionedMe = false,
                        likeOwnerIds = emptyList(),
                        users = emptyMap(),
                        likes = 0,
                        shares = 0,
                        views = 0,
                        likedByMe = false,
                        sharedByMe = false,
                        video = null,
                        attachment = null,
                        comments = 0,
                        commentByMe = false,
                    )

                    repository.save(postToSave, attachmentFile, attachmentType)

                    _postCreated.value = Unit
                    clearNewPostState()
                }
            } catch (e: Exception) {
                handleError(e, R.string.error_save_post)
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    fun edit(postId: Long, newText: String) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.edit(postId, newText.trim())
            } catch (e: Exception) {
                handleError(e, R.string.error_edit_post)
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    private fun handleError(e: Throwable, @StringRes defaultMessageRes: Int) {
        val defaultMessage = appContext.getString(defaultMessageRes)
        val errorMessage = when (e) {
            is HttpException -> {
                when (e.code()) {
                    400 -> appContext.getString(R.string.error_bad_request)
                    401 -> appContext.getString(R.string.error_unauthorized)
                    403 -> appContext.getString(R.string.error_forbidden)
                    404 -> appContext.getString(R.string.error_not_found)
                    500 -> appContext.getString(R.string.error_server)
                    502 -> appContext.getString(R.string.error_gateway)
                    503 -> appContext.getString(R.string.error_unavailable)
                    else -> appContext.getString(
                        R.string.error_with_code_message,
                        e.code(),
                        defaultMessage,
                    )
                }
            }

            is IOException -> appContext.getString(R.string.error_no_internet)
            else -> {
                val message = e.message
                if (!message.isNullOrEmpty()) {
                    "$defaultMessage: $message"
                } else {
                    defaultMessage
                }
            }
        }
        _errorMessage.postValue(errorMessage)
    }

    fun changePhoto(uri: Uri, file: File) {
        _mediaAttachment.value = null
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun setMediaAttachment(attachment: NewPostAttachment) {
        _photo.value = null
        _mediaAttachment.value = attachment
    }

    fun removeMediaAttachment() {
        _mediaAttachment.value = null
    }

    fun setCoords(coords: Coordinates?) {
        _coords.value = coords
    }

    fun setMentionIds(ids: Set<Long>) {
        _mentionIds.value = ids
    }

    fun clearNewPostState() {
        clearDraft()
        _photo.value = null
        _mediaAttachment.value = null
        _coords.value = null
        _mentionIds.value = emptySet()
    }

    fun showAttachmentTooLargeError() {
        _errorMessage.postValue(appContext.getString(R.string.error_attachment_too_large))
    }

    fun checkAuthBeforeAction(action: () -> Unit) {
        if (appAuth.authState.value == null) {
            _shouldAuthenticate.call()
        } else {
            action()
        }
    }

    fun saveWithCheck(text: String) {
        checkAuthBeforeAction { save(text) }
    }

    fun likeWithCheck(id: Long) {
        checkAuthBeforeAction { like(id) }
    }

    fun unlikeWithCheck(id: Long) {
        checkAuthBeforeAction { unlike(id) }
    }

}