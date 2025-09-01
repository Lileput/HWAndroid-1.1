package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post

class PostRepositorySQLiteImpl(private val dao: PostDao) : PostRepository {

    private var posts = emptyList<Post>()
    private val data = MutableLiveData(posts)

    init {
        loadPosts()
    }

    override fun get(): LiveData<List<Post>> = data

    private fun loadPosts() {
        posts = dao.getAll()
        data.value = posts
    }

    override fun likeById(id: Long) {
        dao.likeById(id)
        loadPosts()
    }

    override fun reposts(id: Long) {
        dao.reposts(id)
        loadPosts()
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
        loadPosts()
    }

    override fun save(post: Post) {
        dao.save(post)
        loadPosts()
    }

    override fun edit(postId: Long, content: String) {
        dao.edit(postId, content)
        loadPosts()
    }
}