package ir.hamedan.budgetmanagement.data.local.dao

import androidx.room.*
import ir.hamedan.budgetmanagement.data.local.models.UpcomingPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingPaymentDao {

    @Query("SELECT * FROM upcoming_payments ORDER BY dueDate ASC")
    fun getAllUpcomingPayments(): Flow<List<UpcomingPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: UpcomingPaymentEntity)

    @Update
    suspend fun updatePayment(payment: UpcomingPaymentEntity)

    @Query("DELETE FROM upcoming_payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("UPDATE upcoming_payments SET isPaid = :isPaid WHERE id = :id")
    suspend fun updatePaymentStatus(id: String, isPaid: Boolean)
}