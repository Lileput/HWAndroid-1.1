package ru.netology.nmedia.viewModel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Job
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.repository.JobRepository
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.UserRepository
import ru.netology.nmedia.repository.WallRepository
import java.io.IOException
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val wallRepository: WallRepository,
    private val jobRepository: JobRepository,
    private val postRepository: PostRepository,
    private val appAuth: AppAuth,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: 0L

    val isOwnProfile: Boolean
        get() {
            val authId = appAuth.authState.value?.resolvedId() ?: return false
            if (userId != 0L && userId == authId) return true
            return _user.value?.id == authId
        }

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    val wallData: Flow<PagingData<Post>> = appAuth.authState.flatMapLatest { token ->
        wallRepository.getPagingData(userId).map { pagingData ->
            pagingData.map { post ->
                post.copy(ownedByMe = post.authorId == token?.resolvedId())
            }
        }
    }.flowOn(Dispatchers.Default)

    private val _jobs = MutableLiveData<List<Job>>(emptyList())
    val jobs: LiveData<List<Job>> = _jobs

    private val _jobsLoading = MutableLiveData(false)
    val jobsLoading: LiveData<Boolean> = _jobsLoading

    private val _jobsError = MutableLiveData<String?>(null)
    val jobsError: LiveData<String?> = _jobsError

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadUser()
        loadJobs()
    }

    fun loadUser() {
        viewModelScope.launch {
            try {
                _user.value = userRepository.getById(userId)
            } catch (e: Exception) {
                handleError(e, R.string.error_loading_profile)
            }
        }
    }

    fun loadJobs() {
        viewModelScope.launch {
            _jobsLoading.value = true
            _jobsError.value = null
            try {
                _jobs.value = if (isOwnProfile) {
                    jobRepository.fetchMyJobs()
                } else {
                    jobRepository.fetchUserJobs(userId)
                }
            } catch (e: Exception) {
                _jobsError.value = appContext.getString(R.string.error_loading_jobs)
                handleError(e, R.string.error_loading_jobs)
            } finally {
                _jobsLoading.value = false
            }
        }
    }

    fun removeJob(id: Long) {
        viewModelScope.launch {
            try {
                jobRepository.removeById(id)
                loadJobs()
            } catch (e: Exception) {
                handleError(e, R.string.error_delete_job)
            }
        }
    }

    fun like(id: Long) {
        viewModelScope.launch {
            try {
                postRepository.likeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_like)
            }
        }
    }

    fun unlike(id: Long) {
        viewModelScope.launch {
            try {
                postRepository.unlikeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_unlike)
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                postRepository.removeById(id)
            } catch (e: Exception) {
                handleError(e, R.string.error_delete_post)
            }
        }
    }

    fun repost(id: Long) {
        viewModelScope.launch {
            try {
                postRepository.reposts(id)
            } catch (_: Exception) {
            }
        }
    }

    private fun handleError(e: Throwable, @StringRes defaultMessageRes: Int) {
        val defaultMessage = appContext.getString(defaultMessageRes)
        val message = when (e) {
            is HttpException -> appContext.getString(R.string.error_with_code, e.code())
            is IOException -> appContext.getString(R.string.network_error)
            else -> e.message ?: defaultMessage
        }
        _errorMessage.value = message
    }
}
