package ru.netology.nmedia.viewModel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.HttpException
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

private val empty = Post(
    id = 0,
    author = "",
    content = "",
    published = 0,
    likes = 0,
    likedByMe = false,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val prefs = application.getSharedPreferences("draft_prefs", Context.MODE_PRIVATE)
    private val draftKey = "post_draft"

    init {
        load()
    }


    fun load() {

        _data.postValue(FeedModel(loading = true))
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.value = FeedModel(posts = posts, empty = posts.isEmpty())
            }

            override fun onError(e: Throwable) {
                handleError(e, "Не удалось загрузить посты")
            }
        })
    }

    fun saveDraft(content: String) {
        prefs.edit().putString(draftKey, content).apply()
    }

    fun getDraft(): String? = prefs.getString(draftKey, null)

    fun clearDraft() {
        prefs.edit().remove(draftKey).apply()
    }

    fun like(id: Long) {
        repository.likeById(id, object : PostRepository.RepositoryCallback<Unit> {
            override fun onSuccess(data: Unit) {
                load()
            }

            override fun onError(e: Exception) {
                handleError(e, "Не удалось поставить лайк")
            }
        })
    }

    fun unlike(id: Long) {
        repository.unlikeById(id, object : PostRepository.RepositoryCallback<Unit> {
            override fun onSuccess(data: Unit) {
                load()
            }

            override fun onError(e: Exception) {
                handleError(e, "Не удалось убрать лайк")
            }
        })
    }

    fun reposts(id: Long) {
        try {
            repository.reposts(id)
        } catch (e: Exception) {
            _data.postValue(FeedModel(error = true))
        }
    }

    fun removeById(id: Long) {
        try {
            repository.removeById(id)
            println("DEBUG: Post $id deleted successfully")
            load()
        } catch (e: Exception) {
            handleError(e, "Не удалось удалить пост")
        }
    }

    fun save(text: String) {
        val content = text.trim()
        if (content.isNotEmpty()) {
            try {
                val postToSave = Post(
                    id = 0L,
                    author = "Me",
                    content = content,
                    published = System.currentTimeMillis() / 1000,
                    likes = 0,
                    likedByMe = false,
                    share = 0,
                    views = 0,
                    shareByMe = false,
                    video = null
                )

                repository.save(postToSave)
                _postCreated.value = Unit
                clearDraft()
            } catch (e: Exception) {
                handleError(e, "Не удалось сохранить пост")
            }
        }
    }

    fun edit(postId: Long, newText: String) {
        try {
            repository.edit(postId, newText.trim())
        } catch (e: Exception) {
            _data.value = FeedModel(error = true)
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
            else -> "$defaultMessage: ${e.message}"
        }

        _data.postValue(
            FeedModel(
                error = true,
                posts = _data.value?.posts ?: emptyList()
            )
        )

        Toast.makeText(getApplication(), errorMessage, Toast.LENGTH_LONG).show()
    }
}