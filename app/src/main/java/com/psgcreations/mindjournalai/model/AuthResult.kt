package com.psgcreations.mindjournalai.model

sealed class AuthResult {
    object Success : AuthResult()
    object UserNotFound : AuthResult()
    data class Error(val message: String) : AuthResult()
}
