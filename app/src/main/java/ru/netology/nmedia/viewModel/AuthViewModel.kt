package ru.netology.nmedia.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth,
): ViewModel() {
    val data = appAuth
        .authState
        .asLiveData()

    val isAuthorized: Boolean
        get() = data.value != null
}