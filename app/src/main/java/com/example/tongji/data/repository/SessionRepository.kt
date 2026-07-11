package com.example.tongji.data.repository

import android.util.Log
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

        Log.d("SessionRepository", "完整 API 响应: $body")

        // 检查响应码（成功时 code 为 200）
        val code = body["code"] as? Number
        if (code?.toInt() != 200) {
            val msg = body["msg"] as? String ?: "Unknown error"
            throw Exception("API error ($code): $msg")
        }

        // 从正确的路径提取数据：优先从 data.user 中获取
        val userData = (body["data"] as? Map<String, Any>)?.get("user") as? Map<String, Any>
            ?: (body["data"] as? Map<String, Any>)
            ?: body["user"] as? Map<String, Any>
            ?: body

        Log.d("SessionRepository", "提取到的 userData keys: ${userData.keys.joinToString()}")

        // 详细打印 userData 中的每个字段
        userData.forEach { (key, value) ->
            Log.d("SessionRepository", "  userData[$key] = $value")
        }

        // 检查嵌套的 extend 对象
        val extend = userData["extend"] as? Map<String, Any>
        if (extend != null) {
            Log.d("SessionRepository", "extend 对象存在，keys: ${extend.keys.joinToString()}")
        }

        val nameFromApi = userData["name"] as? String
        val nameFromStore = credentialStore.getString(CredentialStore.KEY_NAME)
        Log.d("SessionRepository", "refreshSessionUser: API返回name='$nameFromApi', 缓存中name='$nameFromStore'")

        val profile = UserProfile(
            uid = userData["uid"] as? String ?: (body["data"] as? Map<String, Any>)?.get("uid") as? String ?: credentialStore.getString(CredentialStore.KEY_UID) ?: "",
            name = nameFromApi ?: nameFromStore ?: "",
            facultyName = userData["facultyName"] as? String,
            deptOrMajor = extend?.get("deptOrMajor") as? String,
            grade = userData["grade"] as? String,
            sexCode = userData["sex"]?.toString(),
            typeCode = userData["type"]?.toString(),
            innerRoles = userData["innerRoles"] as? List<String>,
            photoPath = extend?.get("photoPath") as? String
        )

        credentialStore.putString(CredentialStore.KEY_UID, profile.uid)
        credentialStore.putString(CredentialStore.KEY_NAME, profile.name)
        Log.d("SessionRepository", "已更新 KEY_NAME: '${profile.name}'")
        profile.facultyName?.let { credentialStore.putString(CredentialStore.KEY_FACULTY, it) }
        profile.deptOrMajor?.let { credentialStore.putString(CredentialStore.KEY_MAJOR, it) }
        profile.grade?.let { credentialStore.putString(CredentialStore.KEY_GRADE, it) }
        profile.photoPath?.let { credentialStore.putString(CredentialStore.KEY_PHOTO_PATH, it) }

        CampusModel.updateProfile(profile)
        profile
    }
}
