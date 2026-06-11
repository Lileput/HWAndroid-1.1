package ru.netology.nmedia.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.dto.User
import ru.netology.nmedia.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class PostLikersViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load(userIds: LongArray) {
        if (userIds.isEmpty()) {
            _users.value = emptyList()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                val idSet = userIds.toSet()
                val ordered = userRepository.getAll()
                    .filter { it.id in idSet }
                    .sortedBy { userIds.indexOf(it.id) }
                _users.value = ordered
            } catch (_: Exception) {
                _users.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
