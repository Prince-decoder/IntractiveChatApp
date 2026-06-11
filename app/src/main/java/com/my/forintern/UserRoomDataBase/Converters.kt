package com.my.forintern.UserRoomDataBase

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.forintern.Message.ChatMessage

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromChatMessagesList(messages: List<ChatMessage>?): String? {
        if (messages == null) return null
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        return gson.toJson(messages, type)
    }

    @TypeConverter
    fun toChatMessagesList(messagesString: String?): List<ChatMessage>? {
        if (messagesString == null) return null
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        return gson.fromJson(messagesString, type)
    }
}
