package ru.netology.nmedia.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryFilesImpl
import ru.netology.nmedia.repository.PostRepositoryInMemory
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
    private val repository: PostRepository = PostRepositoryFilesImpl(application)
    val data = repository.get()

    fun like(id: Long) = repository.likeById(id)
    fun reposts(id: Long) = repository.reposts(id)
    fun removeById(id: Long) = repository.removeById(id)

    fun save(text: String) {
        val content = text.trim()
        if (content.isNotEmpty()) {
            repository.save(empty.copy(content = content))
        }
    }

    fun edit(postId: Long, newText: String) {
        repository.edit(postId, newText.trim())
    }
}