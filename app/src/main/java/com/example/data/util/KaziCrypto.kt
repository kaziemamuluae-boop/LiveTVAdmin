package com.example.data.util

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object KaziCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_PHRASE = "KaziTV_Secure_Secret_Key_2026_@_Dhaka!"
    private const val IV_PHRASE = "KaziTV_IV_Vector"

    private val secretKey: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(KEY_PHRASE.toByteArray(Charsets.UTF_8))
        SecretKeySpec(keyBytes, "AES")
    }

    private val ivParameterSpec: IvParameterSpec by lazy {
        val digest = MessageDigest.getInstance("MD5")
        val ivBytes = digest.digest(IV_PHRASE.toByteArray(Charsets.UTF_8))
        IvParameterSpec(ivBytes)
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64Custom.encode(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            val decodedBytes = Base64Custom.decode(encryptedText)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails, maybe it's not encrypted yet (fallback to original text)
            encryptedText
        }
    }

    // A self-contained pure-Kotlin Base64 implementation that works consistently
    // on both JVM (unit tests) and Android devices of all API levels.
    private object Base64Custom {
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

        fun encode(bytes: ByteArray): String {
            val sb = java.lang.StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b1 = bytes[i++].toInt() and 0xFF
                if (i < bytes.size) {
                    val b2 = bytes[i++].toInt() and 0xFF
                    if (i < bytes.size) {
                        val b3 = bytes[i++].toInt() and 0xFF
                        sb.append(CHARS[b1 shr 2])
                        sb.append(CHARS[((b1 and 0x03) shl 4) or (b2 shr 4)])
                        sb.append(CHARS[((b2 and 0x0F) shl 2) or (b3 shr 6)])
                        sb.append(CHARS[b3 and 0x3F])
                    } else {
                        sb.append(CHARS[b1 shr 2])
                        sb.append(CHARS[((b1 and 0x03) shl 4) or (b2 shr 4)])
                        sb.append(CHARS[(b2 and 0x0F) shl 2])
                        sb.append('=')
                    }
                } else {
                    sb.append(CHARS[b1 shr 2])
                    sb.append(CHARS[(b1 and 0x03) shl 4])
                    sb.append("==")
                }
            }
            return sb.toString()
        }

        fun decode(str: String): ByteArray {
            val s = str.replace("\\s".toRegex(), "")
            val len = s.length
            if (len == 0) return ByteArray(0)
            var padding = 0
            if (s[len - 1] == '=') {
                padding++
                if (s[len - 2] == '=') padding++
            }
            val outLen = (len * 3 / 4) - padding
            val out = ByteArray(outLen)
            var i = 0
            var j = 0
            while (i < len) {
                val c1 = CHARS.indexOf(s[i++])
                val c2 = CHARS.indexOf(s[i++])
                val c3 = if (i < len && s[i] != '=') CHARS.indexOf(s[i++]) else { i++; -1 }
                val c4 = if (i < len && s[i] != '=') CHARS.indexOf(s[i++]) else { i++; -1 }

                if (j < outLen) out[j++] = ((c1 shl 2) or (c2 shr 4)).toByte()
                if (j < outLen && c3 != -1) out[j++] = (((c2 and 0x0F) shl 4) or (c3 shr 2)).toByte()
                if (j < outLen && c4 != -1) out[j++] = (((c3 and 0x03) shl 6) or c4).toByte()
            }
            return out
        }
    }
}
