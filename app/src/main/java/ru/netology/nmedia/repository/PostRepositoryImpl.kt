package ru.netology.nmedia.repository


import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post

class PostRepositoryImpl() : PostRepository {


    override fun get(): List<Post> = PostApi.service.getAll()
        .execute()
        .body() ?: error("Body is null!")


    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        PostApi.service.getAll()
            .enqueue(object : retrofit2.Callback<List<Post>> {
                override fun onResponse(
                    call: retrofit2.Call<List<Post>>,
                    response: retrofit2.Response<List<Post>>
                ) {
                    if (response.isSuccessful) {
                        callback.onSuccess(response.body() ?: emptyList())
                    } else {
                        callback.onError(HttpException(response))
                    }
                }

                override fun onFailure(call: retrofit2.Call<List<Post>>, e: Throwable) {
                    callback.onError(e)
                }

            })

    }

    override fun likeById(id: Long, callback: PostRepository.RepositoryCallback<Unit>) {
        try {
            val response = PostApi.service.likeById(id).execute()
            if (response.isSuccessful) {
                callback.onSuccess(Unit)
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    override fun unlikeById(id: Long, callback: PostRepository.RepositoryCallback<Unit>) {
        try {
            val response = PostApi.service.unlikeById(id).execute()
            if (response.isSuccessful) {
                callback.onSuccess(Unit)
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    override fun reposts(id: Long) {
        val response = PostApi.service.reposts(id).execute()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    override fun save(post: Post) {
        val response = PostApi.service.save(post).execute()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    override fun removeById(id: Long) {
        val response = PostApi.service.removeById(id).execute()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    override fun edit(postId: Long, content: String) {
        //dao.edit(postId, content)
    }
}