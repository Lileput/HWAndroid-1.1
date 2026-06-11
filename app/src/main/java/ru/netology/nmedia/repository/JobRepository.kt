package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Job

interface JobRepository {
    val data: Flow<List<Job>>
    suspend fun getUserJobs(userId: Long)
    suspend fun fetchUserJobs(userId: Long): List<Job>
    suspend fun fetchMyJobs(): List<Job>
    suspend fun getMyJobs()
    suspend fun save(job: Job)
    suspend fun removeById(id: Long)
}