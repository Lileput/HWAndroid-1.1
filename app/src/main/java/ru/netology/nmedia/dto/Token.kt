package ru.netology.nmedia.dto

import com.google.gson.annotations.SerializedName

data class Token(
    val id: Long = 0,
    @SerializedName("userId")
    val userId: Long = 0,
    val token: String,
    val avatar: String? = null,
) {
    fun resolvedId(): Long = id.takeIf { it != 0L } ?: userId
}