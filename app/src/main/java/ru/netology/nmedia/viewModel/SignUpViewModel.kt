package ru.netology.nmedia.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val apiService: PostApiService,
    private val appAuth: AppAuth
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun registration(login: String, pass: String, name: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.registration(login, pass, name)

                if (!response.isSuccessful) {
                    _error.value = when (response.code()) {
                        400 -> "Пользователь с таким логином уже существует"
                        else -> "Ошибка ${response.code()}: ${response.message()}"
                    }
                    return@launch
                }

                val regToken = response.body()

                delay(10000)

                var loginSuccess = false
                var loginError: String? = null

                for (attempt in 1..3) {
                    try {
                        val loginResponse = apiService.authentication(login, pass)

                        if (loginResponse.isSuccessful) {
                            val loginToken = loginResponse.body()
                            if (loginToken != null) {
                                appAuth.setAuth(loginToken)
                                loginSuccess = true
                                break
                            }
                        } else {
                            loginError = when (loginResponse.code()) {
                                401 -> "Неверный логин или пароль"
                                403 -> "Доступ запрещён"
                                else -> "Ошибка ${loginResponse.code()}"
                            }
                        }
                    } catch (e: Exception) {
                        loginError = "Ошибка сети: ${e.message}"
                    }

                    if (!loginSuccess && attempt < 3) {
                        delay(3000)
                    }
                }

                if (loginSuccess) {
                    _success.value = true
                } else {
                    if (regToken != null) {
                        appAuth.setAuth(regToken)
                        _error.value =
                            "Регистрация успешна, но автоматический вход не удался. Попробуйте войти вручную."
                        _success.value = true
                    } else {
                        _error.value = loginError ?: "Неизвестная ошибка при входе"
                    }
                }

            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}