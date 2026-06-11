package ru.netology.nmedia.viewModel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.model.UsersModelState
import ru.netology.nmedia.repository.UserRepository
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _data = MutableLiveData<List<User>>(emptyList())
    val data: LiveData<List<User>> = _data

    private val _state = MutableLiveData(UsersModelState(loading = true))
    val state: LiveData<UsersModelState> = _state

    private val _errorMessage = SingleLiveEvent<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        loadUsers()
    }

    fun refresh() {
        loadUsers(isRefresh = true)
    }

    private fun loadUsers(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = UsersModelState(
                loading = !isRefresh,
                refreshing = isRefresh,
                error = false,
            )
            try {
                val users = repository.getAll()
                _data.value = users
                _state.value = UsersModelState()
            } catch (e: Exception) {
                _state.value = UsersModelState(error = true)
                _errorMessage.value = appContext.getString(R.string.error_load_users)
            }
        }
    }
}
