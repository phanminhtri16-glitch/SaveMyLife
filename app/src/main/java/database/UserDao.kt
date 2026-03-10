package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {

    @Insert
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("UPDATE users SET name = :newName, email = :newEmail WHERE email = :oldEmail")
    suspend fun updateUser(oldEmail: String, newName: String, newEmail: String)

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    suspend fun updatePassword(email: String, newPassword: String)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    // Placeholder queries — updated to use the parameter to satisfy Room's compiler
    @Query("SELECT 0 FROM users WHERE email = :email")
    suspend fun getFavoriteCount(email: String): Int

    @Query("SELECT 0 FROM users WHERE email = :email")
    suspend fun getPlayCount(email: String): Int
}