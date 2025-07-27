package ru.netology.nmedia.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryInMemory

class PostViewModel: ViewModel() {
    private val repository : PostRepository = PostRepositoryInMemory()
    val data : LiveData<Post> = repository.get()
    fun like() = repository.like()
    fun reposts() = repository.reposts()
}