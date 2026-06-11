package com.my.forintern.OnBoarding

data class UserProfile(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val traits: Set<String> = emptySet()
)