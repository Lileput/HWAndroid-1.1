package ru.netology.nmedia.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryFilesImpl
import ru.netology.nmedia.repository.PostRepositoryInMemory
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.repository.PostRepositorySharedPrefsImpl

private val empty = Post(
    id = 0,
    author = "",
    content = "",
    published = "",
    likes = 0,
    likeByMe = false,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositorySQLiteImpl(
        AppDb.getInstance(application).postDao
    )
    val data = repository.get()

    private val prefs = application.getSharedPreferences("draft_prefs", Context.MODE_PRIVATE)
    private val draftKey = "post_draft"

    fun saveDraft(content: String) {
        prefs.edit().putString(draftKey, content).apply()
    }

    fun getDraft(): String? = prefs.getString(draftKey, null)

    fun clearDraft() {
        prefs.edit().remove(draftKey).apply()
    }

    fun like(id: Long) = repository.likeById(id)
    fun reposts(id: Long) = repository.reposts(id)
    fun removeById(id: Long) = repository.removeById(id)

    fun save(text: String) {
        val content = text.trim()
        if (content.isNotEmpty()) {
            repository.save(empty.copy(
                content = content,
                author = "Me",
                published = "Now"
            ))
            clearDraft()
        }
    }

    fun edit(postId: Long, newText: String) {
        repository.edit(postId, newText.trim())
    }
}