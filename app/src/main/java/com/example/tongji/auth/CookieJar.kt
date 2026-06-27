package com.example.tongji.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar as OkHttpCookieJar
import okhttp3.HttpUrl

class CookieJar private constructor(context: Context) : OkHttpCookieJar {

    private val prefs: SharedPreferences
    private val gson = Gson()

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "cookie_jar",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString("cookies", null) ?: return
        try {
            val type = object : TypeToken<Map<String, List<CookieBean>>>() {}.type
            val beans: Map<String, List<CookieBean>> = gson.fromJson(json, type)
            cookieStore.clear()
            for ((domain, cookies) in beans) {
                cookieStore[domain] = cookies.map { it.toCookie() }.toMutableList()
            }
        } catch (_: Exception) { }
    }

    private fun saveToPrefs() {
        val beans = mutableMapOf<String, List<CookieBean>>()
        for ((domain, cookies) in cookieStore) {
            beans[domain] = cookies.map { CookieBean.fromCookie(it) }
        }
        prefs.edit().putString("cookies", gson.toJson(beans)).apply()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = url.host
        val existing = cookieStore.getOrPut(domain) { mutableListOf() }
        for (cookie in cookies) {
            existing.removeAll { it.name == cookie.name }
            existing.add(cookie)
        }
        saveToPrefs()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val domain = url.host
        val result = mutableListOf<Cookie>()
        result.addAll(cookieStore[domain] ?: emptyList())
        if (domain != "1.tongji.edu.cn") {
            result.addAll(cookieStore["1.tongji.edu.cn"] ?: emptyList())
        }
        return result.filter { it.expiresAt > System.currentTimeMillis() }
    }

    fun getCookieValue(domain: String, name: String): String? {
        return cookieStore[domain]?.find { it.name == name }?.value
    }

    fun clear() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }

    companion object {
        @Volatile
        private var instance: CookieJar? = null

        fun getInstance(context: Context): CookieJar {
            return instance ?: synchronized(this) {
                instance ?: CookieJar(context.applicationContext).also { instance = it }
            }
        }
    }
}

private data class CookieBean(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val persistent: Boolean,
    val hostOnly: Boolean
) {
    companion object {
        fun fromCookie(cookie: Cookie): CookieBean {
            return CookieBean(
                name = cookie.name,
                value = cookie.value,
                domain = cookie.domain,
                path = cookie.path,
                expiresAt = cookie.expiresAt,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                persistent = cookie.persistent,
                hostOnly = cookie.hostOnly
            )
        }
    }

    fun toCookie(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .path(path)
            .expiresAt(expiresAt)
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        if (hostOnly) builder.hostOnlyDomain(domain)
        return builder.build()
    }
}
