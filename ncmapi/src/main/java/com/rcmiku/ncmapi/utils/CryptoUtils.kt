package com.rcmiku.ncmapi.utils

import java.util.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    // 网易云音乐私有 API 协议规范要求：固定对称密钥与 ECB/CBC 分组加密模式
    private const val PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val IV = "0102030405060708"
    private const val LINUXAPI_KEY = "rFgB&h#%2?^eDg:Q"
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val PUBLIC_KEY_PEM = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ37BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLsvaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9SdB1Ua44oncaTWz7OBGLbCiK45wIDAQAB"
    private const val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val secureRandom = SecureRandom()

    fun weapi(text: String): Map<String, String> {
        val secretKey = createSecretKey(16)
        val params = aesEncrypt(
            aesEncrypt(text, PRESET_KEY, IV),
            secretKey,
            IV
        )
        val encSecKey = rsaEncrypt(secretKey.reversed(), PUBLIC_KEY_PEM)
        
        return mapOf(
            "params" to params,
            "encSecKey" to encSecKey
        )
    }

    fun linuxapi(text: String): Map<String, String> {
        val eparams = aesEncryptHexEcb(text, LINUXAPI_KEY)
        return mapOf("eparams" to eparams)
    }

    fun eapi(url: String, text: String): Map<String, String> {
        val message = "nobody${url}use${text}md5forencrypt"
        val digest = md5Hex(message)
        val data = "${url}-36cd479b6b5-${text}-36cd479b6b5-${digest}"
        val params = aesEncryptHexEcb(data, EAPI_KEY)
        return mapOf("params" to params)
    }

    fun eapiDecryptParams(paramsHex: String): String {
        val plain = aesDecryptHexEcb(paramsHex, EAPI_KEY)
        // plain format: {url}-36cd479b6b5-{json}-36cd479b6b5-{md5}
        val parts = plain.split("-36cd479b6b5-")
        if (parts.size < 3) return plain
        return parts[1]
    }

    private fun aesEncrypt(text: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivParameterSpec = IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    // AES/ECB 模式为网易云 LINUXAPI / EAPI 协议强制规范要求
    private fun aesEncryptHexEcb(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return encryptedBytes.joinToString(separator = "") { b ->
            String.format("%02X", b)
        }
    }

    private fun aesDecryptHexEcb(hex: String, key: String): String {
        val bytes = hex
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decrypted = cipher.doFinal(bytes)
        return decrypted.toString(Charsets.UTF_8)
    }

    private fun rsaEncrypt(text: String, publicKey: String): String {
        val keyBytes = Base64.getDecoder().decode(publicKey)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKey = keyFactory.generatePublic(keySpec)

        val modulus = (pubKey as java.security.interfaces.RSAPublicKey).modulus
        val exponent = pubKey.publicExponent
        
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val bigIntData = BigInteger(1, textBytes)
        val encryptedBigInt = bigIntData.modPow(exponent, modulus)
        
        var hex = encryptedBigInt.toString(16)
        while (hex.length < 256) {
            hex = "0$hex"
        }
        return hex
    }

    private fun createSecretKey(length: Int): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(BASE62[secureRandom.nextInt(BASE62.length)])
        }
        return sb.toString()
    }

    private fun md5Hex(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { b -> String.format("%02x", b) }
    }
}
