package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.remote.api.WaterApi
import com.example.tongji.data.remote.model.WaterController
import com.example.tongji.data.remote.model.WaterGroup
import com.example.tongji.util.WaterCipher
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "WaterRepository"

class WaterRepository(
    private val waterApi: WaterApi,
    private val credentialStore: CredentialStore
) {

    data class WaterAuthParams(
        val account: String,
        val aesKey: String,
        val password: String
    )

    private var cachedParams: WaterAuthParams? = null
    private var cachedToken: String? = null

    init {
        // Try to load persisted params
        loadPersistedParams()
    }

    fun setAuthParams(params: WaterAuthParams) {
        cachedParams = params
        cachedToken = null // invalidate token
        // Persist params
        credentialStore.putString(CredentialStore.KEY_WATER_ACCOUNT, params.account)
        credentialStore.putString(CredentialStore.KEY_WATER_AES_KEY, params.aesKey)
        credentialStore.putString(CredentialStore.KEY_WATER_PASSWORD, params.password)
        Log.d(TAG, "认证参数已持久化")
    }

    fun hasAuthParams(): Boolean {
        return cachedParams != null || credentialStore.contains(CredentialStore.KEY_WATER_ACCOUNT)
    }

    private fun loadPersistedParams() {
        val account = credentialStore.getString(CredentialStore.KEY_WATER_ACCOUNT)
        val aesKey = credentialStore.getString(CredentialStore.KEY_WATER_AES_KEY)
        val password = credentialStore.getString(CredentialStore.KEY_WATER_PASSWORD)
        if (account != null && aesKey != null && password != null) {
            cachedParams = WaterAuthParams(account, aesKey, password)
            Log.d(TAG, "已从持久化存储加载认证参数")
        }
    }

    fun clearPersistedParams() {
        cachedParams = null
        cachedToken = null
        credentialStore.remove(CredentialStore.KEY_WATER_ACCOUNT)
        credentialStore.remove(CredentialStore.KEY_WATER_AES_KEY)
        credentialStore.remove(CredentialStore.KEY_WATER_PASSWORD)
        Log.d(TAG, "认证参数已清除")
    }

    suspend fun fetchGroups(): Result<List<WaterGroup>> = runCatching {
        val resp = waterApi.getGroups()
        val body = resp.body()
            ?: throw Exception("Empty response")
        if (body.retNo != 0) {
            throw Exception(body.retDsp ?: "获取分组失败")
        }
        body.list ?: emptyList()
    }

    suspend fun fetchControllers(group: WaterGroup): Result<List<WaterController>> = runCatching {
        val params = cachedParams ?: throw Exception("未设置认证参数")
        val token = getToken(params)

        val infoObj = JSONObject().apply {
            put("ano", params.account)
            put("groupid", group.classNo)
        }
        val info = WaterCipher.encryptInfo(infoObj, params.aesKey)

        val resp = waterApi.getControllers(info, token)
        val body = resp.body()
            ?: throw Exception("Empty response")
        if (body.retNo != 0) {
            throw Exception(body.retDsp ?: "获取控水器失败")
        }
        body.list ?: emptyList()
    }

    private suspend fun getToken(params: WaterAuthParams): String {
        cachedToken?.let { return it }

        val now = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val infoObj = JSONObject().apply {
            put("userid", params.account)
            put("userpassword", params.password)
            put("time", now)
        }
        val info = WaterCipher.encryptInfo(infoObj, params.aesKey)

        val resp = waterApi.getToken(info)
        val body = resp.body()
            ?: throw Exception("GetToken empty response")
        if (body.retNo != 0) {
            throw Exception(body.retDsp ?: "获取 Token 失败")
        }
        val token = body.token ?: throw Exception("Token 为空")
        cachedToken = token
        return token
    }

    fun clearCache() {
        cachedToken = null
    }

    companion object {
        fun statusText(posNum: Int?): String = when (posNum) {
            0 -> "离线"
            1 -> "空闲"
            2 -> "加锁"
            3 -> "报警"
            4 -> "使用中"
            else -> "未知"
        }
    }
}