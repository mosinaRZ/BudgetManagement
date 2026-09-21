package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getAllUsers(): Flow<List<UserEntity>>

    suspend fun getById(id: String): UserEntity?

    suspend fun getByPhoneNumber(phoneNumber: String): UserEntity?

    suspend fun getLoggedInUser(): UserEntity?

    suspend fun insert(user: UserEntity)

    suspend fun update(user: UserEntity): Int

    suspend fun clearLoggedInState(): Int

    suspend fun setLoggedIn(id: String): Int
}