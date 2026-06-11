package com.my.forintern.UserRoomDataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.my.forintern.Message.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlin.collections.remove
import kotlin.text.set


@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addUser(user: UserDATASET)

    @Query("SELECT * FROM `UserDataSet`")
    abstract fun getAllUsers(): Flow<List<UserDATASET>>

    // 1. Get messages/data of a specific user by ID (Flow for UI observation)
    @Query("SELECT * FROM `UserDataSet` WHERE idphone = :id LIMIT 1")
    abstract fun getUserFlowById(id: Long): Flow<UserDATASET?>


    // Helper method: Get user synchronously for internal DAO use
    @Query("SELECT * FROM `UserDataSet` WHERE idphone = :id LIMIT 1")
    abstract suspend fun getUserByIdSync(id: Long): UserDATASET?

    @Update
    abstract suspend fun updateUser(user: UserDATASET) // Added parameter


    // 2. Add a message to a specific user
    @Transaction
    open suspend fun addMessageToUser(userId: Long, newMessage: ChatMessage) {
        // Fetch the user
        val user = getUserByIdSync(userId)

        if (user != null) {
            val updatedMessages = user.message.toMutableList()
            updatedMessages.add(newMessage)

            user.message = updatedMessages
            updateUser(user)
        }
    }

    // 3. Delete a message from a specific user
    @Transaction
    open suspend fun deleteMessageFromUser(userId: Long, messageToDelete: ChatMessage) {
        val user = getUserByIdSync(userId)

        if (user != null) {
            val updatedMessages = user.message.toMutableList()
            updatedMessages.remove(messageToDelete)

            user.message = updatedMessages
            updateUser(user)
        }
    }

    // 4. Edit a message for a specific user
    @Transaction
    open suspend fun editMessageForUser(userId: Long, oldMessage: ChatMessage, newMessage: ChatMessage) {
        val user = getUserByIdSync(userId)

        if (user != null) {
            val updatedMessages = user.message.toMutableList()
            val index = updatedMessages.indexOf(oldMessage)
            if (index != -1) {
                updatedMessages[index] = newMessage
                user.message = updatedMessages
                updateUser(user)
            }
        }
    }

    // 5. Add new user or update existing while keeping history
    @Transaction
    open suspend fun addOrUpdateUserKeepHistory(user: UserDATASET) {
        val existingUser = getUserByIdSync(user.idphone)
        if (existingUser != null) {
            user.message = existingUser.message
            updateUser(user)
        } else {
            addUser(user)
        }
    }
}