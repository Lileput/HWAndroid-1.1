package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.JobDao
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.entity.JobEntity
import ru.netology.nmedia.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val dao: JobDao,
    private val apiService: PostApiService,
) : JobRepository {

    override val data = dao.getAll().map { list ->
        list.map { it.toDto() }
    }

    override suspend fun getUserJobs(userId: Long) {
        fetchUserJobs(userId)
    }

    override suspend fun fetchUserJobs(userId: Long): List<Job> {
        try {
            val response = apiService.getUserJobs(userId)
            if (!response.isSuccessful) throw error("Response error")
            return response.body().orEmpty()
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }

    override suspend fun fetchMyJobs(): List<Job> {
        try {
            val response = apiService.getMyJobs()
            if (!response.isSuccessful) throw error("Response error")
            return response.body().orEmpty()
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }

    override suspend fun getMyJobs() {
        try {
            val body = fetchMyJobs()
            dao.clear()
            dao.insert(body.map { JobEntity.fromDto(it) })
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }

    override suspend fun save(job: Job) {
        try {
            val response = apiService.saveJob(job)
            if (!response.isSuccessful) throw error("Response error")
            val body = response.body() ?: throw error("Body is null")
            dao.insert(JobEntity.fromDto(body))
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeJobById(id)
            if (!response.isSuccessful) throw error("Response error")
            dao.removeById(id)
        } catch (e: Exception) {
            throw AppError.from(e)
        }
    }
}