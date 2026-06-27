package com.example.tongji.util

import android.util.Base64
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object StudentCodeCipher {

    fun encryptStudentCode(uid: String, aesKey: String, aesIv: String): String {
        val processedKey = paramHandler(aesKey)
        val processedIv = paramHandler(aesIv)

        val encodedText = URLEncoder.encode(uid, "UTF-8")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(processedKey.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(processedIv.toByteArray(Charsets.UTF_8))
        )
        val encrypted = cipher.doFinal(encodedText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun paramHandler(input: String): String {
        val chars = input.toCharArray()
        var i = 0
        while (i + 1 < chars.size) {
            val temp = chars[i]
            chars[i] = chars[i + 1]
            chars[i + 1] = temp
            i += 2
        }
        return String(chars)
    }
}
