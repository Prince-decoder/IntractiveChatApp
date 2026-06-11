package com.my.forintern
import android.content.Context
import androidx.room.Room
import com.my.forintern.UserRoomDataBase.UserDataBase
import com.my.forintern.UserRoomDataBase.UserRepository

object Graph {
    lateinit var userdatabase: UserDataBase

    val userrepo by lazy {
        UserRepository(userDao = userdatabase.userDao())
    }

    fun provide(context: Context)
    {
        userdatabase = Room.databaseBuilder(context = context,
            klass = UserDataBase::class.java, name = "userdatabase").build()
    }
}