package ru.netology.nmedia.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.dto.Token

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
    }

    @Synchronized
    fun setAuth(token: Token) {
        _authState.value = token
        prefs.edit{
            putLong(ID_KEY, token.id)
            putString(TOKEN_KEY, token.token)
        }
    }

    @Synchronized
    fun clear() {
        _authState.value = null
        prefs.edit { clear() }
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