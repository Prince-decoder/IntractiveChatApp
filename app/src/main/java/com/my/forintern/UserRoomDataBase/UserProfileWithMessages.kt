package com.my.forintern.UserRoomDataBase

import androidx.room.Embedded
import androidx.room.Relation
import com.my.forintern.Message.ChatMessage
import com.my.forintern.OnBoarding.UserProfile

data class UserProfileWithMessages(
    @Embedded val userProfile: UserProfile,
    @Relation(
        parentColumn = "phone",
        entityColumn = "userPhone"
    )
    val messages: List<ChatMessage>
)
