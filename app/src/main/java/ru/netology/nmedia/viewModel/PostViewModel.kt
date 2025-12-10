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

        _data.postValue(FeedModel(loading = true))
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
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
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun unlike(id: Long) {
        repository.unlikeById(id, object : PostRepository.RepositoryCallback<Unit> {
            override fun onSuccess(data: Unit) {
                load()
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun reposts(id: Long) = repository.reposts(id)
    fun removeById(id: Long) {
        repository.removeById(id, object : PostRepository.RepositoryCallback<Unit> {
            override fun onSuccess(data: Unit) {
                println("DEBUG: Post $id deleted successfully")
                load()
            }

            override fun onError(e: Exception) {
                e.printStackTrace()
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun save(text: String) {
        val content = text.trim()
        if (content.isNotEmpty()) {
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

            repository.save(postToSave, object : PostRepository.RepositoryCallback<Post> {
                override fun onSuccess(data: Post) {
                    _postCreated.postValue(Unit)
                    clearDraft()
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            })
        }
    }

    fun edit(postId: Long, newText: String) {
        repository.edit(postId, newText.trim())
    }
}