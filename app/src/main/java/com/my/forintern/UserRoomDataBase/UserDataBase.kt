package com.my.forintern.UserRoomDataBase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserDATASET::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UserDataBase: RoomDatabase() {
    abstract fun userDao(): UserDao
}