package com.example.tongji.util

import android.util.Base64
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object WaterCipher {

    /**
     * AES-ECB-PKCS7 (PKCS5 in JVM) encrypt then Base64 encode.
     * @param obj JSON object to encrypt
     * @param keyB64 AES key in Base64 format
     * @return Base64 encoded ciphertext
     */
    fun encryptInfo(obj: JSONObject, keyB64: String): String {
        val plaintext = obj.toString().toByteArray(Charsets.UTF_8)
        val key = Base64.decode(keyB64, Base64.DEFAULT)
        val padLen = 16 - (plaintext.size % 16)
        val padded = plaintext + ByteArray(padLen) { padLen.toByte() }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val ciphertext = cipher.doFinal(padded)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }
}