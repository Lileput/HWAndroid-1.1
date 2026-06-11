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
class ChooseUsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val selectedIds = linkedSetOf<Long>()

    fun initSelection(initial: Set<Long>) {
        selectedIds.clear()
        selectedIds.addAll(initial)
    }

    fun loadUsers() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _users.value = userRepository.getAll()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun isSelected(userId: Long): Boolean = userId in selectedIds

    fun toggle(userId: Long) {
        if (userId in selectedIds) {
            selectedIds.remove(userId)
        } else {
            selectedIds.add(userId)
        }
    }

    fun selectedIds(): Set<Long> = selectedIds.toSet()
}
