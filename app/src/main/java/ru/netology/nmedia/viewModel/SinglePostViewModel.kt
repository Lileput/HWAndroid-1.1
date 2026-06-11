package ru.netology.nmedia.viewModel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SinglePostViewModel @Inject constructor(
    private val apiService: PostApiService,
    private val repository: PostRepository,
    private val appAuth: AppAuth,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    fun repost(postId: Long) {
        viewModelScope.launch {
            try {
                repository.reposts(postId)
            } catch (_: Exception) {
            }
        }
    }

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load(postId: Long) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = apiService.getById(postId)
                if (!response.isSuccessful) {
                    _error.value = appContext.getString(R.string.error_with_code, response.code())
                    _post.value = null
                    return@launch
                }
                val body = response.body()
                if (body == null) {
                    _error.value = appContext.getString(R.string.post_not_found)
                    _post.value = null
                    return@launch
                }
                val currentUserId = appAuth.authState.value?.resolvedId()
                _post.value = body.copy(
                    ownedByMe = body.authorId == currentUserId,
                    likes = body.likesCount,
                )
            } catch (_: IOException) {
                _error.value = appContext.getString(R.string.network_error)
            } catch (e: HttpException) {
                _error.value = appContext.getString(R.string.error_with_code, e.code())
            } catch (e: Exception) {
                _error.value = e.message ?: appContext.getString(R.string.error_unknown)
            } finally {
                _loading.value = false
            }
        }
    }
}
