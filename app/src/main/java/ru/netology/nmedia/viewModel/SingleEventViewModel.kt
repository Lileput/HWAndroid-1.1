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
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Event
import ru.netology.nmedia.repository.EventRepository
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SingleEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val appAuth: AppAuth,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load(eventId: Long) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val body = repository.getById(eventId)
                if (body == null) {
                    _error.value = appContext.getString(R.string.event_not_found)
                    _event.value = null
                    return@launch
                }
                val currentUserId = appAuth.authState.value?.resolvedId()
                _event.value = body.copy(ownedByMe = body.authorId == currentUserId)
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
