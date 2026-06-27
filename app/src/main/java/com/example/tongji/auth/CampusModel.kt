package com.example.tongji.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CampusModel {
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun markValid() { _authState.value = AuthState.Valid }
    fun markRenewing() { _authState.value = AuthState.Renewing }
    fun markRecoverableExpired() { _authState.value = AuthState.ExpiredRecoverable }
    fun markRequiresInteractiveLogin() { _authState.value = AuthState.RequiresInteractiveLogin }
    fun markLoggedOut() { _authState.value = AuthState.LoggedOut }

    fun updateProfile(profile: UserProfile) { _userProfile.value = profile }
    fun clearProfile() { _userProfile.value = null }
}

data class UserProfile(
    val uid: String,
    val name: String,
    val facultyName: String?,
    val deptOrMajor: String?,
    val grade: String?,
    val sexCode: String?,
    val typeCode: String?,
    val innerRoles: List<String>?,
    val photoPath: String?
)
