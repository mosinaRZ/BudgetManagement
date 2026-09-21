package ir.hamedan.budgetmanagement.data.repository

interface AuthRepository {
    suspend fun login(identifier: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isAuthenticated(): Boolean
}