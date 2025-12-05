package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.toEntity
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

    override fun likeById(id: Long) {
        val request = Request.Builder()
            .url("${BASE_URL}api/posts/$id/likes")
            .post("".toRequestBody(jsonType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val stringBody = response.body?.string()
            println("DEBUG: Like response: $stringBody")
        } catch (e: Exception) {
            println("DEBUG: Like error: ${e.message}")
        }
    }

    override fun unlikeById(id: Long) {
        val request = Request.Builder()
            .url("${BASE_URL}api/posts/$id/likes")
            .delete()
            .build()

        try {
            val response = client.newCall(request).execute()
            val stringBody = response.body?.string()
            println("DEBUG: Unlike response: $stringBody")
        } catch (e: Exception) {
            println("DEBUG: Unlike error: ${e.message}")
        }
    }

    override fun reposts(id: Long) {
        //dao.reposts(id)
    }

    override fun removeById(id: Long) {

        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts/$id")
            .delete("".toRequestBody(jsonType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val stringBody = response.body?.string()
            println("DEBUG: Delete response: $stringBody")
        } catch (e: Exception) {
            println("DEBUG: Delete error: ${e.message}")
        }
    }

    override fun save(post: Post): Post {

        val json = gson.toJson(post)
        val requestBody = json.toRequestBody(jsonType)

        val request = Request.Builder()
            .url("${BASE_URL}api/slow/posts")
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        val response = call.execute()

        val stringBody = response.body.string()

        return gson.fromJson(stringBody, Post::class.java)
    }

    override fun edit(postId: Long, content: String) {
        //dao.edit(postId, content)
    }
}