package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM PostEntity WHERE hidden = 0 ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAllIncludingHidden(): Flow<List<PostEntity>>

    @Query("SELECT * FROM PostEntity WHERE isNew = 1 AND hidden = 1 ORDER BY id DESC")
    fun getNewPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    suspend fun getByIdSync(id: Long): PostEntity?

    @Insert
    suspend fun insert(post: PostEntity)

    @Insert
    suspend fun insert(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM PostEntity")
    suspend fun clear()

    @Query("SELECT MAX(id) FROM PostEntity")
    suspend fun getMaxId(): Long?

    suspend fun clearAndInsert(posts: List<PostEntity>) {
        clear()
        insert(posts)
    }

    @Query("UPDATE PostEntity SET content = :content WHERE id = :id")
    suspend fun edit(id: Long, content: String)

    @Query(
        """
        UPDATE PostEntity SET
        likes = likes + CASE WHEN likedByMe = 1 THEN -1 ELSE 1 END,
        likedByMe = CASE WHEN likedByMe = 1 THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
        shares = shares + 1,
        sharedByMe = 1
        WHERE id = :id
        """
    )
    suspend fun reposts(id: Long)

    @Query("UPDATE PostEntity SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: PostEntity.SyncStatus)

    @Query("UPDATE PostEntity SET isNew = :isNew WHERE id = :id")
    suspend fun updateIsNew(id: Long, isNew: Boolean)

    @Query("UPDATE PostEntity SET hidden = :hidden WHERE id = :id")
    suspend fun updateHidden(id: Long, hidden: Boolean)

    @Query("UPDATE PostEntity SET hidden = 0 WHERE hidden = 1")
    suspend fun showAllHiddenPosts()

    @Query("SELECT COUNT(*) FROM PostEntity WHERE isNew = 1 AND hidden = 1")
    suspend fun getNewHiddenPostsCount(): Int

    @Query("UPDATE PostEntity SET isNew = 0")
    suspend fun markAllAsNotNew()

    @Query("SELECT COUNT(*) FROM PostEntity WHERE isNew = 1")
    suspend fun getNewPostsCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM PostEntity WHERE id = :id)")
    suspend fun postExists(id: Long): Boolean
}