package com.my.forintern.FireDatabase

import com.google.firebase.firestore.PropertyName

data class UserData(
    @get:PropertyName("Name") @set:PropertyName("Name") var firstName: String = "",
    @get:PropertyName("Phone") @set:PropertyName("Phone") var phone: Long = 0L,
    @get:PropertyName("Message") @set:PropertyName("Message") var message: String =""
)
{
    @get:PropertyName("Name") @set:PropertyName("Name")
    var legacyName: String
        get() = firstName
        set(value) { if (value.isNotEmpty()) firstName = value }

    @get:PropertyName("Phone") @set:PropertyName("Phone")
    var legacyPhone: Long
        get() = phone
        set(value) { if (value != 0L) phone = value } // Used 0L check since Long doesn't have isNotEmpty()

    @get:PropertyName("Message") @set:PropertyName("Message")
    var legacyMessage: String
        get() = message
        set(value) { if (value.isNotEmpty()) message = value }
}