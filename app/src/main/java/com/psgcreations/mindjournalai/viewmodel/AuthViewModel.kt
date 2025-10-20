package com.psgcreations.mindjournalai.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psgcreations.mindjournalai.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.psgcreations.mindjournalai.model.AuthResult

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val isLoginSuccess = mutableStateOf(false)
//    val isRegisterSuccess = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // New state to trigger redirect to register
    val redirectToRegister = mutableStateOf<String?>(null)
    var googleRedirectToRegister = mutableStateOf(false)

    fun resetGoogleRedirect() {
        googleRedirectToRegister.value = false
    }


    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            Log.d("GoogleChecks123", "signInWithGoogle: $result")
            when (result) {
                is AuthResult.Success -> isLoginSuccess.value = true
                is AuthResult.UserNotFound -> googleRedirectToRegister.value = true
                is AuthResult.Error -> errorMessage.value = result.message
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            when (val result = repository.signInWithEmail(email, password)) {
                is AuthResult.Success -> isLoginSuccess.value = true
                is AuthResult.UserNotFound -> redirectToRegister.value = email
                is AuthResult.Error -> errorMessage.value = result.message
            }
        }
    }

    fun registerUser(name: String, email: String, password: String, mobile: String) {
        viewModelScope.launch {
            val result = repository.registerUser(name, email, password, mobile)
            if (result) {
                isLoginSuccess.value = true
            } else {
                errorMessage.value = "Registration failed"
            }
        }
    }

    // Reset redirect state after navigation
    fun resetRedirect() {
        redirectToRegister.value = null
    }
}
