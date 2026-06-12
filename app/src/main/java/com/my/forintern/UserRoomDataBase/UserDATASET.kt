package com.my.forintern.UserRoomDataBase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.my.forintern.Message.ChatMessage

@Entity(tableName = "UserDataSet")
data class UserDATASET(

    @PrimaryKey
    var idphone: Long=0L,
    @ColumnInfo
    var sender:String="",
    @ColumnInfo
    var message: List<ChatMessage>,
)