package com.my.forintern.FireDatabase

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.my.forintern.Graph
import com.my.forintern.UserRoomDataBase.Converters
import com.my.forintern.FireDatabase.UserData
import com.my.forintern.FireDatabase.Injection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class FirebaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val roomRepo = Graph.userrepo
            val firestore = Injection.instance()
            val converters = Converters()

            // Fetch all users from Room
            val localUsers = roomRepo.getalluser().firstOrNull() ?: return Result.success()

            // Upload each to Firebase
            for (localUser in localUsers) {
                val messageString = converters.fromChatMessagesList(localUser.message)
                
                val userData = UserData(
                    firstName = localUser.sender,
                    phone = localUser.idphone,
                    message = messageString ?: ""
                )

                // Try to set it to Firestore
                firestore.collection("Customer").document(userData.phone.toString())
                    .set(userData)
                    .await()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<FirebaseSyncWorker>()
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "FirebaseSync",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
