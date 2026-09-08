package com.example.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    private var isFirebaseAvailable: Boolean = false

    fun init(context: Context) {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            isFirebaseAvailable = app != null
        } catch (_: Exception) {
            isFirebaseAvailable = false
        }
    }

    fun isAvailable(): Boolean = isFirebaseAvailable

    fun getAuth(): FirebaseAuth? {
        return if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    fun getStorage(): FirebaseStorage? {
        return if (isFirebaseAvailable) {
            try {
                FirebaseStorage.getInstance()
            } catch (_: Exception) {
                null
            }
        } else null
    }
}
