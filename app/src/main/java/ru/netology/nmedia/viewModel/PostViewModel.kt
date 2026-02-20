package ru.netology.nmedia.viewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl(
        AppDb.getInstance(application).postDao()
    )

    val data: LiveData<FeedModel> = repository.data.map { list: List<Post> -> FeedModel(list, list.isEmpty())}
        .catch { it.printStackTrace() }
        .asLiveData(Dispatchers.Default)

    val newerCount = repository.getNewer(0)
        .catch { _state.postValue(FeedModelState(error = true)) }
        .asLiveData(Dispatchers.Default)

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState>
        get() = _state

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _errorMessage = SingleLiveEvent<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val prefs = application.getSharedPreferences("draft_prefs", Context.MODE_PRIVATE)
    private val draftKey = "post_draft"

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?>
        get() = _photo

    init {
        load()
        observeNewPosts()
    }


    fun load() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(loading = true)
                repository.getAll()
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
                handleError(e, "Не удалось загрузить данные")
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

    fun refresh() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.getAll()
                _state.value = FeedModelState()
            } catch (e: Exception) {
                _state.value = FeedModelState(error = true)
                handleError(e, "Не удалось обновить данные")
            }
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
                _state.value = FeedModelState(refreshing = true)
                repository.likeById(id)
            } catch (e: Exception) {
                handleError(e, "Не удалось поставить лайк")
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    fun unlike(id: Long) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.unlikeById(id)
            } catch (e: Exception) {
                handleError(e, "Не удалось убрать лайк")
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    fun reposts(id: Long) {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState(refreshing = true)
                repository.reposts(id)
            } catch (e: Exception) {
                handleError(e, "Не удалось сделать репост")
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
                handleError(e, "Не удалось удалить пост")
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

                    val postToSave = Post(
                        id = 0L,
                        author = "Me",
                        authorAvatar = null,
                        content = content,
                        published = (System.currentTimeMillis() / 1000),
                        likes = 0,
                        shares = 0,
                        views = 0,
                        likedByMe = false,
                        sharedByMe = false,
                        video = null,
                        attachment = null,
                        comments = 0,
                        commentByMe = false
                    )

                    repository.save(postToSave, _photo.value?.file)

                    _postCreated.value = Unit
                    clearDraft()
                    _photo.value = null
                }
            } catch (e: Exception) {
                handleError(e, "Не удалось сохранить пост")
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
                handleError(e, "Не удалось отредактировать пост")
            } finally {
                _state.value = FeedModelState()
            }
        }
    }

    private fun handleError(e: Throwable, defaultMessage: String) {
        val errorMessage = when (e) {
            is HttpException -> {
                when (e.code()) {
                    400 -> "Неверный запрос"
                    401 -> "Требуется авторизация"
                    403 -> "Доступ запрещен"
                    404 -> "Не найдено"
                    500 -> "Ошибка сервера"
                    502 -> "Проблема с шлюзом"
                    503 -> "Сервис недоступен"
                    else -> "$defaultMessage (код ${e.code()})"
                }
            }
            is IOException -> "Проблема с интернет-соединением"
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
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }
}