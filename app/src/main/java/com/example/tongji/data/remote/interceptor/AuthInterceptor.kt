package com.example.tongji.data.remote.interceptor

import com.example.tongji.auth.CookieJar
import com.example.tongji.auth.CredentialStore
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject

class AuthInterceptor(
    private val cookieJar: CookieJar,
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        val builder = original.newBuilder()

        when {
            host == "yikatong.tongji.edu.cn" || host.endsWith(".yikatong.tongji.edu.cn") -> {
                val cookies = cookieJar.loadForRequest(original.url)
                if (cookies.isNotEmpty()) {
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    builder.header("Cookie", cookieStr)
                }
            }
            host == "all.tongji.edu.cn" || host.endsWith(".all.tongji.edu.cn") -> {
                val cookies = cookieJar.loadForRequest(original.url)
                if (cookies.isNotEmpty()) {
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    builder.header("Cookie", cookieStr)
                }
            }
            host.contains("1.tongji.edu.cn") -> {
                val cookies = cookieJar.loadForRequest(original.url)
                if (cookies.isNotEmpty()) {
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    builder.header("Cookie", cookieStr)
                }
                val sessionId = credentialStore.getString(CredentialStore.KEY_SESSION_ID)
                if (sessionId != null) {
                    builder.header("X-Token", sessionId)
                } else {
                    val uid = credentialStore.getString(CredentialStore.KEY_UID)
                    if (uid != null) {
                        builder.header("X-Token", uid)
                    }
                }
            }
            host.contains("star.tongji.edu.cn") -> {
                val token = credentialStore.getString(CredentialStore.KEY_STAR_BEARER)
                if (token != null) {
                    builder.header("Authorization", "Bearer $token")
                }
            }
            host.contains("space.tongji.edu.cn") -> {
                val cookies = cookieJar.loadForRequest(original.url)
                if (cookies.isNotEmpty()) {
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    builder.header("Cookie", cookieStr)
                }
                val jwt = credentialStore.getString(CredentialStore.KEY_LIBRARY_JWT)
                if (jwt != null) {
                    builder.header("Authorization", "bearer$jwt")
                    original.body?.let { body ->
                        val ct = body.contentType()
                        if (ct != null && ct.subtype.contains("json")) {
                            injectAuthIntoBody(builder, body, ct, jwt)
                        }
                    }
                }
                builder.header("lang", "zh")
                builder.header("X-Requested-With", "XMLHttpRequest")
            }
            host == "ks.tongji.edu.cn" || host.endsWith(".ks.tongji.edu.cn") -> {
                val cookies = cookieJar.loadForRequest(original.url)
                if (cookies.isNotEmpty()) {
                    val cookieStr = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    builder.header("Cookie", cookieStr)
                }
                builder.header("Accept", "application/json, text/plain, */*")
                builder.header("Referer", "https://ks.tongji.edu.cn/")
            }
        }

        return chain.proceed(builder.build())
    }

    private fun injectAuthIntoBody(
        builder: okhttp3.Request.Builder,
        body: okhttp3.RequestBody,
        contentType: MediaType,
        jwt: String
    ) {
        try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyStr = buffer.readUtf8()
            val json = if (bodyStr.isEmpty()) JSONObject() else JSONObject(bodyStr)
            if (!json.has("authorization")) {
                json.put("authorization", "bearer$jwt")
                builder.method("POST", RequestBody.create(contentType, json.toString()))
            }
        } catch (_: Exception) { }
    }
}
