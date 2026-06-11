package ru.netology.nmedia.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.dto.Media

interface EventApiService {
    @GET("events/latest")
    suspend fun getLatest(@Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/before")
    suspend fun getBefore(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/after")
    suspend fun getAfter(@Path("id") id: Long, @Query("count") count: Int): Response<List<Event>>

    @GET("events/{id}/newer")
    suspend fun getNewer(@Path("id") id: Long): Response<List<Event>>

    @GET("events/{id}")
    suspend fun getById(@Path("id") id: Long): Response<Event>

    @POST("events")
    suspend fun save(@Body event: Event): Response<Event>

    @DELETE("events/{id}")
    suspend fun removeById(@Path("id") id: Long): Response<Unit>

    @POST("events/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Response<Unit>

    @DELETE("events/{id}/likes")
    suspend fun unlikeById(@Path("id") id: Long): Response<Unit>

    @POST("events/{id}/participants")
    suspend fun participate(@Path("id") id: Long): Response<Unit>

    @DELETE("events/{id}/participants")
    suspend fun unparticipate(@Path("id") id: Long): Response<Unit>
}
