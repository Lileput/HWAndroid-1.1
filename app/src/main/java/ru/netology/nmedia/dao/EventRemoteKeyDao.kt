package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.EventRemoteKeyEntity

@Dao
interface EventRemoteKeyDao {
    @Query("SELECT key FROM EventRemoteKeyEntity WHERE type = :type")
    suspend fun getKey(type: EventRemoteKeyEntity.KeyType): Long?

    suspend fun getAfter(): Long? = getKey(EventRemoteKeyEntity.KeyType.AFTER)

    suspend fun getBefore(): Long? = getKey(EventRemoteKeyEntity.KeyType.BEFORE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: EventRemoteKeyEntity)
}
