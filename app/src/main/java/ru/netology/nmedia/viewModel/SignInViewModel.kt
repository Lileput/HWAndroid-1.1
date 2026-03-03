package ru.netology.nmedia.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.auth.AppAuth
import java.io.IOException

class SignInViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun authentication(login: String, pass: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = PostApi.service.authentication(login, pass)

                if (!response.isSuccessful) {
                    _error.value = when (response.code()) {
                        400 -> "Неверный логин или пароль"
                        else -> "Ошибка ${response.code()}: ${response.message()}"
                    }
                    return@launch
                }

                val token = response.body()
                if (token != null) {
                    AppAuth.getInstance().setAuth(token)
                    _success.value = true
                } else {
                    _error.value = "Пустой ответ от сервера"
                }

            } catch (e: IOException) {
                _error.value = "Ошибка сети: ${e.message}"
            } catch (e: HttpException) {
                _error.value = "Ошибка сервера: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Неизвестная ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}