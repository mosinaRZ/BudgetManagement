package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String = "",
    val fullName: String = "",
    val email: String? = null,
    val profileImageUri: String? = null,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis() // معادل RealmInstant.now() در Realm
)