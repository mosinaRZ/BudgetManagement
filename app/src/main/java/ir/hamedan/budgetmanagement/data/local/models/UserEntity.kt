package ir.hamedan.budgetmanagement.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["phoneNumber"], unique = true),
        Index(value = ["updatedAt"])
    ]
)
data class UserEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val phoneNumber: String = "",

    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "prefer_not_to_say",
    val birthDate: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,

    /**
     * Indicates the currently active local account.
     */
    val isLoggedIn: Boolean = false,

    /**
     * Local/account creation time.
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last local modification time.
     */
    val updatedAt: Long = System.currentTimeMillis()
)