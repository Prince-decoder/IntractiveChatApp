package com.my.forintern.Message

data class ChatMessage(
    val text: String = "",
    val time:String="",
    val issentByme: Boolean=false
)