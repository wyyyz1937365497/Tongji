package com.example.tongji.auth

sealed class AuthState {
    data object LoggedOut : AuthState()
    data object Valid : AuthState()
    data object Renewing : AuthState()
    data object ExpiredRecoverable : AuthState()
    data object RequiresInteractiveLogin : AuthState()

    val isLoggedIn: Boolean get() = this !is LoggedOut
}
