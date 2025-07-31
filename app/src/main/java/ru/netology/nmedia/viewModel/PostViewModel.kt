package ru.netology.nmedia.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryInMemory

class PostViewModel: ViewModel() {
    private val repository : PostRepository = PostRepositoryInMemory()
    val data = repository.get()
    fun like(id: Long) = repository.likeById(id)
    fun reposts(id: Long) = repository.reposts(id)
}