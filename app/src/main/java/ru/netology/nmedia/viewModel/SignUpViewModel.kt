package ru.netology.nmedia.viewModel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import ru.netology.nmedia.R
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
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

    fun registration(login: String, pass: String, name: String, avatarFile: File) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _toastMessage.value = null

                val avatarPart = MultipartBody.Part.createFormData(
                    "file",
                    avatarFile.name,
                    avatarFile.asRequestBody("image/*".toMediaTypeOrNull()),
                )

                val response = apiService.registration(
                    login = login,
                    pass = pass,
                    name = name,
                    file = avatarPart,
                )

                if (!response.isSuccessful) {
                    when (response.code()) {
                        400, 403 -> _toastMessage.value =
                            appContext.getString(R.string.user_already_registered)
                        415 -> _toastMessage.value =
                            appContext.getString(R.string.error_avatar_invalid)
                        else -> _error.value = appContext.getString(
                            R.string.error_with_code_message,
                            response.code(),
                            response.message().orEmpty(),
                        )
                    }
                    return@launch
                }

                val regToken = response.body()

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
                                401 -> appContext.getString(R.string.wrong_login_or_password)
                                403 -> appContext.getString(R.string.error_forbidden)
                                else -> appContext.getString(R.string.error_with_code, loginResponse.code())
                            }
                        }
                    } catch (e: Exception) {
                        loginError = appContext.getString(R.string.network_error)
                    }
                }

                if (loginSuccess) {
                    mergeAvatarFromRegistration(regToken)
                    _success.value = true
                } else if (regToken != null) {
                    appAuth.setAuth(regToken)
                    _success.value = true
                } else {
                    _error.value = loginError ?: appContext.getString(R.string.error_sign_in_after_registration)
                }
            } catch (e: IOException) {
                _error.value = appContext.getString(R.string.network_error)
            } catch (e: HttpException) {
                when (e.code()) {
                    400, 403 -> _toastMessage.value = appContext.getString(R.string.user_already_registered)
                    415 -> _toastMessage.value = appContext.getString(R.string.error_avatar_invalid)
                    else -> _error.value = appContext.getString(R.string.error_server_with_message, e.message.orEmpty())
                }
            } catch (e: Exception) {
                _error.value = appContext.getString(R.string.error_server_with_message, e.message.orEmpty())
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mergeAvatarFromRegistration(regToken: Token?) {
        val current = appAuth.authState.value ?: return
        if (!current.avatar.isNullOrBlank() || regToken?.avatar.isNullOrBlank()) return
        appAuth.setAuth(current.copy(avatar = regToken.avatar))
    }
}
