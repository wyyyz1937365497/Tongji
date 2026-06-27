package com.example.tongji.data.repository

import com.example.tongji.auth.CampusModel
import com.example.tongji.auth.CredentialStore
import com.example.tongji.auth.UserProfile
import com.example.tongji.data.remote.api.TongjiApi

class SessionRepository(
    private val api: TongjiApi,
    private val credentialStore: CredentialStore
) {
    suspend fun refreshSessionUser(): Result<UserProfile> = runCatching {
        val resp = api.getSessionUser()
        val body = resp.body() ?: throw Exception("Empty response")
        val data = body["user"] as? Map<String, Any> ?: body

        val profile = UserProfile(
            uid = data["uid"] as? String ?: credentialStore.getString(CredentialStore.KEY_UID) ?: "",
            name = data["name"] as? String ?: credentialStore.getString(CredentialStore.KEY_NAME) ?: "",
            facultyName = data["facultyName"] as? String ?: credentialStore.getString(CredentialStore.KEY_FACULTY),
            deptOrMajor = data["deptOrMajor"] as? String ?: credentialStore.getString(CredentialStore.KEY_MAJOR),
            grade = data["grade"] as? String ?: credentialStore.getString(CredentialStore.KEY_GRADE),
            sexCode = data["sexCode"] as? String,
            typeCode = data["typeCode"] as? String,
            innerRoles = data["innerRoles"] as? List<String>,
            photoPath = data["photoPath"] as? String ?: credentialStore.getString(CredentialStore.KEY_PHOTO_PATH)
        )

        credentialStore.putString(CredentialStore.KEY_UID, profile.uid)
        credentialStore.putString(CredentialStore.KEY_NAME, profile.name)
        profile.facultyName?.let { credentialStore.putString(CredentialStore.KEY_FACULTY, it) }
        profile.deptOrMajor?.let { credentialStore.putString(CredentialStore.KEY_MAJOR, it) }
        profile.grade?.let { credentialStore.putString(CredentialStore.KEY_GRADE, it) }
        profile.photoPath?.let { credentialStore.putString(CredentialStore.KEY_PHOTO_PATH, it) }

        CampusModel.updateProfile(profile)
        profile
    }
}
