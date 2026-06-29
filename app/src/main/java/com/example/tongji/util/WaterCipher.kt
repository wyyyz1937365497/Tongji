package com.example.tongji.util

import android.util.Base64
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object WaterCipher {

    const val DEFAULT_AES_KEY = "3n4DdO47LWH2Co/WfpbdyA=="
    const val DEFAULT_PASSWORD = "kv7XjPzrDNJY0pdZ#"

    fun encryptInfo(obj: JSONObject, keyB64: String): String {
        val plaintext = obj.toString().toByteArray(Charsets.UTF_8)
        val key = Base64.decode(keyB64, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val ciphertext = cipher.doFinal(plaintext)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private val AES_KEY_REGEX = """["']([A-Za-z0-9+/]{22}==)["']""".toRegex()
    private val PASSWORD_REGEX = """userpassword\s*:\s*["']([^"']{6,100})["']""".toRegex()

    fun findAesKey(jsTexts: List<String>): String? {
        for (text in jsTexts) {
            for (match in AES_KEY_REGEX.findAll(text)) {
                val candidate = match.groupValues[1]
                try {
                    val raw = Base64.decode(candidate, Base64.DEFAULT)
                    if (raw.size in listOf(16, 24, 32)) {
                        val around = text.substring(
                            maxOf(0, match.range.first - 500),
                            minOf(text.length, match.range.last + 1000)
                        )
                        if (around.contains("AES.encrypt") || around.contains("AES.decrypt")) {
                            return candidate
                        }
                    }
                } catch (_: Exception) { }
            }
        }
        return null
    }

    fun findPassword(jsTexts: List<String>): String? {
        for (text in jsTexts) {
            for (match in PASSWORD_REGEX.findAll(text)) {
                val c = match.groupValues[1]
                if (!c.lowercase().startsWith("string")) {
                    return c
                }
            }
        }
        return null
    }
}
