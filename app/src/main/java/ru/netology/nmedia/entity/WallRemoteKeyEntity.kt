package ru.netology.nmedia.entity

import androidx.room.Entity

@Entity(primaryKeys = ["authorId", "type"])
data class WallRemoteKeyEntity(
    val authorId: Long,
    val type: KeyType,
    val key: Long,
) {
    enum class KeyType {
        AFTER,
        BEFORE,
    }
}
