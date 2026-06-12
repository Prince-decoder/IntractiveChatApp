package com.my.forintern.FireDatabase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.firestore.Source
import kotlin.time.Duration.Companion.milliseconds

class UserFRepository(val auth: FirebaseAuth, val firestore: FirebaseFirestore) {

    suspend fun saveUserToDatabase(userDetails: UserData): Results<Boolean> {
        return try {
            val email = auth.currentUser?.phoneNumber ?: return Results.error(Exception("User not present"))
            withTimeout(10_000) {
                firestore.collection("Customer").document(userDetails.phone.toString())
                    .set(userDetails)
                    .await()
            }
            Results.Success(true)
        } catch (e: Exception) {
            Results.error(e)
        }
    }

    suspend fun getCurrentUser(): Results<UserData> {
        return try {
            val phone = auth.currentUser?.phoneNumber ?: return Results.error(Exception("User not present"))
            val snapshot = withTimeout(10_000.milliseconds) {
                firestore.collection("Customer").document(phone)
                    .get(Source.SERVER)
                    .await()
            }

            val user = snapshot.toObject(UserData::class.java)
                ?: return Results.error(Exception("User not found"))

            Results.Success(user)
        } catch (e: Exception) {
            Results.error(e)
        }
    }

    suspend fun getUserByPhone(phone: String): Results<UserData> {
        return try {
            val snapshot = withTimeout(10_000.milliseconds) {
                firestore.collection("Customer").document(phone)
                    .get(Source.SERVER)
                    .await()
            }

            val user = snapshot.toObject(UserData::class.java)
                ?: return Results.error(Exception("User not found"))

            Results.Success(user)
        } catch (e: Exception) {
            Results.error(e)
        }
    }
}