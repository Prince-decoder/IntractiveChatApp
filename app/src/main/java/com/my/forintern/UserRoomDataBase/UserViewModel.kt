package com.my.forintern.UserRoomDataBase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.forintern.Graph
import com.my.forintern.Message.ChatMessage
import com.my.forintern.FireDatabase.FirebaseSyncWorker
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository= Graph.userrepo) : ViewModel() {

    fun adduser(userDATASET: UserDATASET)
    {
        viewModelScope.launch {
            repository.adduser(userDATASET)
            FirebaseSyncWorker.enqueue(Graph.appContext)
        }
    }

    fun addOrUpdateUserKeepHistory(userDATASET: UserDATASET) {
        viewModelScope.launch {
            repository.addOrUpdateUserKeepHistory(userDATASET)
            FirebaseSyncWorker.enqueue(Graph.appContext)
        }
    }

    fun deleteMessage(userId: Long, message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessage(userId, message)
            FirebaseSyncWorker.enqueue(Graph.appContext)
        }
    }

    fun editMessage(userId: Long, oldMessage: ChatMessage, newMessage: ChatMessage) {
        viewModelScope.launch {
            repository.editMessage(userId, oldMessage, newMessage)
            FirebaseSyncWorker.enqueue(Graph.appContext)
        }
    }

    // Add other repository access functions here as needed
}
