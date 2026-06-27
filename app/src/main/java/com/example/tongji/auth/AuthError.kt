package com.example.tongji.auth

sealed class AuthError(message: String) : Exception(message) {
    data object NotLoggedIn : AuthError("Not logged in")
    data object Expired : AuthError("Session expired")
    data object LoginFlowFailed : AuthError("Login flow failed")
    data object MfaRequired : AuthError("MFA required")
}
