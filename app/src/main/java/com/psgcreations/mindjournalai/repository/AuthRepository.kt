package com.psgcreations.mindjournalai.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import com.psgcreations.mindjournalai.model.AuthResult


@ViewModelScoped
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun signInWithEmail(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                AuthResult.Success
            } catch (e: Exception) {
                Log.e("SignIn", "Error: ${e.message}")
                // Check Firebase error type
                return@withContext when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> AuthResult.UserNotFound
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> AuthResult.UserNotFound
                    else -> AuthResult.Error(e.localizedMessage ?: "Unknown error")
                }
            }
        }

    // Register user with additional fields (Name + Mobile)
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        mobile: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@withContext false

                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "mobile" to mobile
                )

                firestore.collection("users")
                    .document(uid)
                    .set(userData)
                    .await()

                true
            } catch (e: Exception) {
                Log.e("Register", "Error: ${e.message}")
                false
            }
        }

    suspend fun signInWithGoogle(context: Context): AuthResult = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("475769241173-r8pn4ls0idvl3k8854stpgqdmar8619u.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val idToken = googleIdTokenCredential.idToken
            val name = googleIdTokenCredential.displayName
            val email = googleIdTokenCredential.id
            val phone = googleIdTokenCredential.phoneNumber

            if (email.isEmpty()) return@withContext AuthResult.Error("Email not available from Google")

            // Authenticate with Firebase
            val firebaseCredential =
                com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val uid = authResult.user?.uid
                ?: return@withContext AuthResult.Error("Firebase sign-in failed")

            // Check Firestore for existing user with this email
            val userQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (userQuery.isEmpty) {
                // User does not exist → add to Firestore
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "mobile" to phone
                )
                firestore.collection("users")
                    .document(uid)
                    .set(userData)
                    .await()
            }

            AuthResult.Success

        } catch (e: Exception) {
            Log.e("GoogleSignIn", e.message, e)
            AuthResult.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    private fun hashNonce(nonce: String): String {
        val bytes = nonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun retrieveAndSaveFcmToken() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        // 1. Get the Token (Existing code)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                firestore.collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }

        // 2. NEW: Subscribe to "daily_reminder" topic
        // This groups all your users into one "bucket" for messaging
        FirebaseMessaging.getInstance().subscribeToTopic("daily_reminder")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to daily_reminder topic")
                } else {
                    Log.e("FCM", "Subscription failed")
                }
            }
    }

}
