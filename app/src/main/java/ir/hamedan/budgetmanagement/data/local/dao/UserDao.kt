package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query(
        """
        SELECT *
        FROM users
        ORDER BY createdAt DESC
        """
    )
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query(
        """
        SELECT *
        FROM users
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: String): UserEntity?

    @Query(
        """
        SELECT *
        FROM users
        WHERE phoneNumber = :phoneNumber
        LIMIT 1
        """
    )
    suspend fun getByPhoneNumber(phoneNumber: String): UserEntity?

    @Query(
        """
        SELECT *
        FROM users
        WHERE isLoggedIn = 1
        LIMIT 1
        """
    )
    suspend fun getLoggedInUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity): Int

    @Query(
        """
        UPDATE users
        SET isLoggedIn = 0,
            updatedAt = :updatedAt
        """
    )
    suspend fun clearLoggedInState(updatedAt: Long): Int

    @Query(
        """
        UPDATE users
        SET isLoggedIn = 1,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setLoggedIn(
        id: String,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM users")
    suspend fun clearAll(): Int
}