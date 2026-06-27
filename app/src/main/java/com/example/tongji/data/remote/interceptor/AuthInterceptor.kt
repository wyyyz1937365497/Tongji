package com.example.tongji.data.remote.interceptor

import com.example.tongji.auth.CookieJar
import com.example.tongji.auth.CredentialStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val cookieJar: CookieJar,
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        val builder = original.newBuilder()

        when {
            host.contains("1.tongji.edu.cn") || host.contains("tongji.edu.cn") -> {
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
            host.contains("pay-yikatong") -> {
                val token = credentialStore.getString(CredentialStore.KEY_YIKATONG_TOKEN)
                if (token != null) {
                    builder.header("synjones-auth", token)
                }
                val cookie = credentialStore.getString(CredentialStore.KEY_YIKATONG_COOKIE)
                if (cookie != null) {
                    builder.header("Cookie", cookie)
                }
            }
            host.contains("space.tongji.edu.cn") -> {
                val jwt = credentialStore.getString(CredentialStore.KEY_LIBRARY_JWT)
                if (jwt != null) {
                    builder.header("Authorization", "Bearer $jwt")
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
