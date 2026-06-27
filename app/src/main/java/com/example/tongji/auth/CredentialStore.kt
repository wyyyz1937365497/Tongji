package com.example.tongji.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStore private constructor(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun contains(key: String): Boolean = prefs.contains(key)

    companion object {
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
        const val KEY_FACULTY = "facultyName"
        const val KEY_MAJOR = "deptOrMajor"
        const val KEY_GRADE = "grade"
        const val KEY_STAR_BEARER = "star_bearer_token"
        const val KEY_LIBRARY_JWT = "library_space_jwt"
        const val KEY_YIKATONG_TOKEN = "yikatong_token"
        const val KEY_YIKATONG_COOKIE = "yikatong_cookie"
        const val KEY_PHOTO_PATH = "photoPath"
        const val KEY_AUTO_LOGIN_USERNAME = "auto_login_username"
        const val KEY_AUTO_LOGIN_PASSWORD = "auto_login_password"

        @Volatile
        private var instance: CredentialStore? = null

        fun getInstance(context: Context): CredentialStore {
            return instance ?: synchronized(this) {
                instance ?: CredentialStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
