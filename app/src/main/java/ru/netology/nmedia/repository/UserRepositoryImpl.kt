package ru.netology.nmedia.repository

import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: PostApiService,
) : UserRepository {
    override suspend fun getAll(): List<User> {
        try {
            val response = apiService.getUsers()
            if (!response.isSuccessful) error("Response error: ${response.code()}")
            return response.body() ?: error("Body is null")
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }

    override suspend fun getById(id: Long): User {
        try {
            val response = apiService.getUserById(id)
            if (!response.isSuccessful) error("Response error: ${response.code()}")
            return response.body() ?: error("Body is null")
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }
}
