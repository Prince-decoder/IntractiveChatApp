package com.my.forintern.OnBoarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserProfileRepository(private val context: Context) {

    companion object {
        val NAME_KEY = stringPreferencesKey("name")
        val AGE_KEY = stringPreferencesKey("age")
        val PHONE_KEY = stringPreferencesKey("phone")
        val TRAITS_KEY = stringSetPreferencesKey("traits")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data
        .map { preferences ->
            val name = preferences[NAME_KEY] ?: ""
            val age = preferences[AGE_KEY] ?: ""
            val phone = preferences[PHONE_KEY] ?: ""
            val traits = preferences[TRAITS_KEY] ?: emptySet()
            UserProfile(name, age, phone, traits)
        }

    suspend fun saveUserProfile(userProfile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[NAME_KEY] = userProfile.name
            preferences[AGE_KEY] = userProfile.age
            preferences[PHONE_KEY] = userProfile.phone
            preferences[TRAITS_KEY] = userProfile.traits
        }
    }
}