package ru.netology.nmedia.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
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
        thread {
            _data.postValue(FeedModel(loading = true))

            try {
                val posts = repository.get()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (_: Exception) {
                _data.postValue(FeedModel(error = true))
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
        thread {
            try {
                repository.likeById(id)
                load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unlike(id: Long) {
        thread {
            try {
                repository.unlikeById(id)
                load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun reposts(id: Long) = repository.reposts(id)
    fun removeById(id: Long) {
        thread {
            try {
                println("DEBUG: ViewModel removing post $id")
                repository.removeById(id)
                load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun save(text: String) {
        thread {
            val content = text.trim()
            if (content.isNotEmpty()) {
                repository.save(
                    empty.copy(
                        content = content,
                        author = "Me",
                    )
                )
                _postCreated.postValue(Unit)
                clearDraft()
            }
        }
    }

    fun edit(postId: Long, newText: String) {
        repository.edit(postId, newText.trim())
    }
}