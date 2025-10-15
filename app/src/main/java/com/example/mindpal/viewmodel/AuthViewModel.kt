package com.example.mindpal.ui.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindpal.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.mindpal.model.AuthResult

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val isLoginSuccess = mutableStateOf(false)
    val isRegisterSuccess = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // New state to trigger redirect to register
    val redirectToRegister = mutableStateOf<String?>(null)

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val token = repository.signInWithGoogle(context)
            if (token != null) {
                isLoginSuccess.value = true
            } else {
                errorMessage.value = "Google sign-in failed"
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
                isRegisterSuccess.value = true
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
