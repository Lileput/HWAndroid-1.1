package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.toEntity

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {

    override fun get(): LiveData<List<Post>> = dao.getAll().map { listPosts ->
        listPosts.map { entity ->
            entity.toDto()
        }
    }

    override fun likeById(id: Long) {
        dao.likeById(id)
    }

    override fun reposts(id: Long) {
        dao.reposts(id)
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
    }

    override fun save(post: Post) {
        dao.save(post.toEntity())
    }

    override fun edit(postId: Long, content: String) {
        dao.edit(postId, content)
    }
}