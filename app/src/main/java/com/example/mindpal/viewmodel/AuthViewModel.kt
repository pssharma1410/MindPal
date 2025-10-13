package com.example.mindpal.ui.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindpal.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val isLoginSuccess = mutableStateOf(false)

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val token = repository.signInWithGoogle(context)
            if (token != null) {
                android.util.Log.i("GoogleToken", token)
                isLoginSuccess.value = true
            } else {
                android.util.Log.e("GoogleToken", "Sign-in failed")
            }
        }
    }
}
