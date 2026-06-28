package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.data.remote.api.WaterApi
import com.example.tongji.data.remote.model.WaterController
import com.example.tongji.data.remote.model.WaterGroup
import com.example.tongji.util.WaterCipher
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "WaterRepository"

class WaterRepository(private val waterApi: WaterApi) {

    data class WaterAuthParams(
        val account: String,
        val aesKey: String,
        val password: String
    )

    private var cachedParams: WaterAuthParams? = null
    private var cachedToken: String? = null

    fun setAuthParams(params: WaterAuthParams) {
        cachedParams = params
        cachedToken = null // invalidate token
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