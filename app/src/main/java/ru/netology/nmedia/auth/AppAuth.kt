package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import kotlin.coroutines.EmptyCoroutineContext

class AppAuth private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authState = MutableStateFlow<Token?>(null)
    val authState = _authState.asStateFlow()

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0)

        if (token == null || id == 0L) {
            prefs.edit { clear() }
        } else {
            _authState.value = Token(id, token)
        }

        sendPushToken()
    }

    @Synchronized
    fun setAuth(token: Token) {
        _authState.value = token
        prefs.edit{
            putLong(ID_KEY, token.id)
            putString(TOKEN_KEY, token.token)
        }
        sendPushToken()
    }

    @Synchronized
    fun clear() {
        _authState.value = null
        prefs.edit { clear() }
        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(EmptyCoroutineContext).launch {
            runCatching {
                PostApi.service.sendPushToken(PushToken(token ?: FirebaseMessaging.getInstance().token.await()))
            }
                .onFailure { it.printStackTrace() }
        }
    }

    companion object {
        private const val TOKEN_KEY = "TOKEN_KEY"
        private const val ID_KEY = "ID_KEY"

        @Volatile
        private var INSTANCE: AppAuth? = null

        fun initApp(context: Context){
            INSTANCE = AppAuth(context)
        }

        fun getInstance(): AppAuth = requireNotNull(INSTANCE) {
            "Should call initApp first!"
        }
    }
}