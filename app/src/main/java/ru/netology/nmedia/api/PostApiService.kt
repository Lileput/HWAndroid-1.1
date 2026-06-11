package ru.netology.nmedia.api


import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.dto.User

interface PostApiService {
    @GET("posts")
    suspend fun getAll(): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatest(@Query("count") count : Int): Response<List<Post>>

    @GET("posts/{id}")
    suspend fun getById(@Path("id") id: Long): Response<Post>

    @GET("posts/{id}/before")
    suspend fun getBefore(@Path("id") id: Long, @Query("count") count : Int): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getAfter(@Path("id") id: Long, @Query("count") count : Int): Response<List<Post>>

    @POST("posts")
    suspend fun save(@Body post: Post): Response<Post>

    @DELETE("posts/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<Unit>

    @DELETE("posts/{id}/likes")
    suspend fun unlikeById(@Path("id") id: Long): Response<Unit>

    @POST("posts/{id}/reposts")
    suspend fun reposts(@Path("id") id: Long): Response<Unit>

    @GET("posts/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<Post>>

    @Multipart
    @POST("media")
    suspend fun upload(@Part file: MultipartBody.Part): Media

    @POST("users/authentication")
    suspend fun authentication(
        @Query("login") login: String,
        @Query("pass") pass: String,
    ): Response<Token>

    @Multipart
    @POST("users/registration")
    suspend fun registration(
        @Query("login") login: String,
        @Query("pass") pass: String,
        @Query("name") name: String,
        @Part file: MultipartBody.Part,
    ): Response<Token>

    @POST("users/push-tokens")
    suspend fun sendPushToken(@Body token: PushToken)

    @GET("{author_id}/wall")
    suspend fun getWall(@Path("author_id") authorId: Long): Response<List<Post>>

    @GET("{author_id}/wall/latest")
    suspend fun getWallLatest(@Path("author_id") authorId: Long, @Query("count") count: Int): Response<List<Post>>

    @GET("{author_id}/wall/{id}/before")
    suspend fun getWallBefore(
        @Path("author_id") authorId: Long,
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{author_id}/wall/{id}/after")
    suspend fun getWallAfter(
        @Path("author_id") authorId: Long,
        @Path("id") id: Long,
        @Query("count") count: Int
    ): Response<List<Post>>

    @GET("{user_id}/jobs")
    suspend fun getUserJobs(@Path("user_id") userId: Long): Response<List<Job>>

    @GET("my/jobs")
    suspend fun getMyJobs(): Response<List<Job>>

    @POST("my/jobs")
    suspend fun saveJob(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{job_id}")
    suspend fun removeJobById(@Path("job_id") jobId: Long): Response<Unit>

    @GET("users")
    suspend fun getUsers(): Response<List<User>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>
}