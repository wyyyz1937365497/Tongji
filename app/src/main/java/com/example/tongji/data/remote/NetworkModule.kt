package com.example.tongji.data.remote

import android.content.Context
import com.example.tongji.auth.CookieJar
import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.remote.api.*
import com.example.tongji.data.remote.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private var cookieJar: CookieJar? = null
    private var credentialStore: CredentialStore? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val cj = CookieJar.getInstance(context)
        val cs = CredentialStore.getInstance(context)
        cookieJar = cj
        credentialStore = cs
        return OkHttpClient.Builder()
            .cookieJar(cj)
            .addInterceptor(AuthInterceptor(cj, cs))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createTongjiApi(context: Context): TongjiApi {
        return Retrofit.Builder()
            .baseUrl("https://1.tongji.edu.cn")
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TongjiApi::class.java)
    }

    fun createStarApi(context: Context): StarApi {
        return Retrofit.Builder()
            .baseUrl("https://star.tongji.edu.cn")
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StarApi::class.java)
    }

    fun createLibrarySpaceApi(context: Context): LibrarySpaceApi {
        return Retrofit.Builder()
            .baseUrl("https://space.tongji.edu.cn")
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibrarySpaceApi::class.java)
    }

    fun createAllTongjiApi(context: Context): AllTongjiApi {
        return Retrofit.Builder()
            .baseUrl("https://all.tongji.edu.cn")
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AllTongjiApi::class.java)
    }

    fun createYikatongApi(context: Context): YikatongApi {
        return Retrofit.Builder()
            .baseUrl("https://yikatong.tongji.edu.cn")
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YikatongApi::class.java)
    }
}
