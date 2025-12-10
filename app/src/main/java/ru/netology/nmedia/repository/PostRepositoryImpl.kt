package ru.netology.nmedia.repository


import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

class PostRepositoryImpl() : PostRepository {

    companion object {
        const val BASE_URL = "http://10.0.2.2:9999/"
        val jsonType = "application/json".toMediaType()
        val typeToken = object : TypeToken<List<Post>>() {}
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun get(): List<Post> {

        val url = "${BASE_URL}api/slow/posts"
        println("DEBUG: Request URL: $url")

        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts")
            .build()

        val call = client.newCall(request)

        val response = call.execute()

        val stringBody = response.body.string()

        return gson.fromJson(stringBody, typeToken.type)
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val posts = response.body?.string() ?: throw RuntimeException("body is null")
                        callback.onSuccess(gson.fromJson(posts, typeToken.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

            })
    }

    override fun likeById(id: Long, callback: PostRepository.RepositoryCallback<Unit>) {
        val request = Request.Builder()
            .url("${BASE_URL}api/posts/$id/likes")
            .post("".toRequestBody(jsonType))
            .build()

       client.newCall(request)
           .enqueue(object : Callback {
               override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
               }

               override fun onResponse(call: Call, response: Response) {
                   try {
                       if (!response.isSuccessful) {
                           throw RuntimeException("HTTP error: ${response.code}")
                       }
                       callback.onSuccess(Unit)
                   } catch (e: Exception) {
                       callback.onError(e)
                   }
               }

           })
    }

    override fun unlikeById(id: Long, callback: PostRepository.RepositoryCallback<Unit>) {
        val request = Request.Builder()
            .url("${BASE_URL}api/posts/$id/likes")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP error: ${response.code}")
                    }
                    callback.onSuccess(Unit)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })
    }

    override fun reposts(id: Long) {
        //dao.reposts(id)
    }

    override fun save(post: Post, callback: PostRepository.RepositoryCallback<Post>) {
        val json = gson.toJson(post)
        val requestBody = json.toRequestBody(jsonType)

        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP error: ${response.code}")
                    }
                    val stringBody = response.body?.string() ?: throw RuntimeException("body is null")
                    val savedPost = gson.fromJson(stringBody, Post::class.java)
                    callback.onSuccess(savedPost)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })
    }

    override fun removeById(id: Long, callback: PostRepository.RepositoryCallback<Unit>) {

        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts/$id")
            .delete("".toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        throw RuntimeException("HTTP error: ${response.code}")
                    }
                    callback.onSuccess(Unit)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })
    }



    override fun edit(postId: Long, content: String) {
        //dao.edit(postId, content)
    }
}