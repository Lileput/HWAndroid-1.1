package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.WallRemoteKeyEntity

@Dao
interface WallRemoteKeyDao {

    @Query("SELECT `key` FROM WallRemoteKeyEntity WHERE authorId = :authorId AND type = 'AFTER'")
    suspend fun getAfter(authorId: Long): Long?

    @Query("SELECT `key` FROM WallRemoteKeyEntity WHERE authorId = :authorId AND type = 'BEFORE'")
    suspend fun getBefore(authorId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WallRemoteKeyEntity)

    @Query("DELETE FROM WallRemoteKeyEntity WHERE authorId = :authorId")
    suspend fun clearForAuthor(authorId: Long)
}
