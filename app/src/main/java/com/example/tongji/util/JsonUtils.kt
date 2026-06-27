package com.example.tongji.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    private val gson = Gson()

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (_: Exception) {
            null
        }
    }

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    fun normalizeJavaScriptValue(value: String): String {
        return value.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
    }

    fun normalizeJsonPayload(payload: String): String {
        var result = payload.trim()
        try {
            val parsed = gson.fromJson(result, Map::class.java)
            if (parsed is Map<*, *> && parsed.size == 1) {
                val firstValue = parsed.values.first()
                if (firstValue is String) {
                    return firstValue
                }
            }
        } catch (_: Exception) {}
        return result
    }
}
