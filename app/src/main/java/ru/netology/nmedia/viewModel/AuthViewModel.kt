package ru.netology.nmedia.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel: ViewModel() {
    val data = AppAuth.getInstance()
        .authState
        .asLiveData()

    val isAuthorized: Boolean
        get() = data.value != null
}