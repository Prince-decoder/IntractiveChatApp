package com.my.forintern

import android.app.Application

class UserDataApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}