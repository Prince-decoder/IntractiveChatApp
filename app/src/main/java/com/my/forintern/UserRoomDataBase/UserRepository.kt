package com.my.forintern.UserRoomDataBase

import com.my.forintern.Message.ChatMessage
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun adduser(user: UserDATASET)
    {
        userDao.addUser(user)
    }

    fun getalluser(): Flow<List<UserDATASET>>
    {
        return userDao.getAllUsers()
    }

    fun getUserFlowById(id: Long): Flow<UserDATASET?> {
        return userDao.getUserFlowById(id)
    }

    suspend fun deleteMessage(userId: Long, message: ChatMessage) {
        userDao.deleteMessageFromUser(userId, message)
    }

    suspend fun editMessage(userId: Long, oldMessage: ChatMessage, newMessage: ChatMessage) {
        userDao.editMessageForUser(userId, oldMessage, newMessage)
    }

    suspend fun addOrUpdateUserKeepHistory(user: UserDATASET) {
        userDao.addOrUpdateUserKeepHistory(user)
    }

}