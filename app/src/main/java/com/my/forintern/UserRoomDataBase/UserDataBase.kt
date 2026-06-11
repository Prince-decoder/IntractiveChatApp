package com.my.forintern.UserRoomDataBase

import androidx.room.Database

@Database(
    entities = [UserDATASET::class],
    version = 1,
    exportSchema = false
)
abstract class UserDataBase {
    abstract fun userDao(): UserDao
}