package com.my.forintern.FireDatabase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserFRepository(val auth: FirebaseAuth,val firestore: FirebaseFirestore) {

    suspend fun saveUserToDatabase(userDetails: UserData): Results<Boolean> {
        return try {
            val email = auth.currentUser?.email ?: return Results.error(Exception("User not present"))
            firestore.collection("Customer").document(email)
                .set(userDetails)
                .await()
            Results.Success(true)
        } catch (e: Exception) {
            Results.error(e)
        }
    }

    suspend fun getCurrentUser(): Results<UserData> {
        return try {
            val email = auth.currentUser?.phoneNumber ?: return Results.error(Exception("User not present"))
            val snapshot = firestore.collection("Customer").document(email)
                .get()
                .await()

            val user = snapshot.toObject(UserData::class.java)
                ?: return Results.error(Exception("User not found"))

            Results.Success(user)
        } catch (e: Exception) {
            Results.error(e)
        }
    }

    suspend fun getUserByPhone(phone: String): Results<UserData> {
        return try {
            val snapshot = firestore.collection("Customer").document(phone)
                .get()
                .await()

            val user = snapshot.toObject(UserData::class.java)
                ?: return Results.error(Exception("User not found"))

            Results.Success(user)
        } catch (e: Exception) {
            Results.error(e)
        }
    }
}