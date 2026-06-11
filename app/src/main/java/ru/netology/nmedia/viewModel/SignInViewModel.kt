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
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val apiService: PostApiService,
    private val appAuth: AppAuth,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _toastMessage = MutableLiveData<String?>(null)
    val toastMessage: LiveData<String?> = _toastMessage

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun onToastShown() {
        _toastMessage.value = null
    }

    fun authentication(login: String, pass: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _toastMessage.value = null

                val response = apiService.authentication(login, pass)

                if (!response.isSuccessful) {
                    when (response.code()) {
                        400 -> _toastMessage.value =
                            appContext.getString(R.string.wrong_login_or_password)
                        else -> _error.value = appContext.getString(
                            R.string.error_with_code_message,
                            response.code(),
                            response.message().orEmpty(),
                        )
                    }
                    return@launch
                }

                val token = response.body()
                if (token != null) {
                    appAuth.setAuth(token)
                    _success.value = true
                } else {
                    _error.value = appContext.getString(R.string.empty_server_response)
                }
            } catch (e: IOException) {
                _error.value = appContext.getString(R.string.network_error)
            } catch (e: HttpException) {
                if (e.code() == 400) {
                    _toastMessage.value = appContext.getString(R.string.wrong_login_or_password)
                } else {
                    _error.value = appContext.getString(R.string.error_server_with_message, e.message.orEmpty())
                }
            } catch (e: Exception) {
                _error.value = appContext.getString(
                    R.string.error_unknown,
                ) + (e.message?.let { ": $it" } ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
