package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.User

interface UserRepository {
    suspend fun getAll(): List<User>
    suspend fun getById(id: Long): User
}
