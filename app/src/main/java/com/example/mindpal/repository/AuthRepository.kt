package com.example.mindpal.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import com.example.mindpal.model.AuthResult


@ViewModelScoped
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun signUpWithEmail(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            Log.e("SignUp", "Error: ${e.message}")
            false
        }
    }


    suspend fun signInWithEmail(email: String, password: String):AuthResult = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("SignIn", "Error: ${e.message}")
            // Check Firebase error type
            return@withContext when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> AuthResult.UserNotFound
                else -> AuthResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // Register user with additional fields (Name + Mobile)
    suspend fun registerUser(name: String, email: String, password: String, mobile: String): Boolean =
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

    suspend fun signInWithGoogle(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("1054859165915-a05ivc2n460raembioi23nfcjjud19dc.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            val credential = result.credential
            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(credential.data)
            return@withContext googleIdTokenCredential.idToken
        } catch (e: Exception) {
            Log.e("GoogleSignIn", e.message, e)
            null
        }
    }

    private fun hashNonce(nonce: String): String {
        val bytes = nonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
