package ir.hamedan.budgetmanagement.data.repository

import ir.hamedan.budgetmanagement.data.local.dao.UserDao
import ir.hamedan.budgetmanagement.data.local.models.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override fun getAllUsers(): Flow<List<UserEntity>> =
        userDao.getAllUsers()

    override suspend fun getById(
        id: String
    ): UserEntity? =
        userDao.getById(id)

    override suspend fun getByPhoneNumber(
        phoneNumber: String
    ): UserEntity? =
        userDao.getByPhoneNumber(phoneNumber)

    override suspend fun getLoggedInUser(): UserEntity? =
        userDao.getLoggedInUser()

    override suspend fun insert(
        user: UserEntity
    ) {
        val now = System.currentTimeMillis()

        userDao.insert(
            user.copy(
                createdAt = if (user.createdAt > 0L) {
                    user.createdAt
                } else {
                    now
                },
                updatedAt = now
            )
        )
    }

    override suspend fun update(
        user: UserEntity
    ): Int =
        userDao.update(
            user.copy(
                updatedAt = System.currentTimeMillis()
            )
        )

    override suspend fun clearLoggedInState(): Int =
        userDao.clearLoggedInState(
            updatedAt = System.currentTimeMillis()
        )

    override suspend fun setLoggedIn(
        id: String
    ): Int =
        userDao.setLoggedIn(
            id = id,
            updatedAt = System.currentTimeMillis()
        )
}