package com.rcmiku.ncmapi

import com.rcmiku.ncmapi.model.FlexibleDoubleSerializer
import com.rcmiku.ncmapi.model.GeneralResponse
import com.rcmiku.ncmapi.model.NcmApiException
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.CryptoUtils
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NcmapiPipelineTest {

    @Test
    fun testFlexibleDoubleSerializer() {
        val jsonInt = """{"id":1,"name":"test","playCount":12345}"""
        val p1 = json.decodeFromString(Playlist.serializer(), jsonInt)
        assertEquals(12345.0, p1.playCount, 0.001)

        val jsonDouble = """{"id":2,"name":"test","playCount":888.5}"""
        val p2 = json.decodeFromString(Playlist.serializer(), jsonDouble)
        assertEquals(888.5, p2.playCount, 0.001)

        val jsonString = """{"id":3,"name":"test","playCount":"9999"}"""
        val p3 = json.decodeFromString(Playlist.serializer(), jsonString)
        assertEquals(9999.0, p3.playCount, 0.001)

        val jsonDefault = """{"id":4,"name":"test"}"""
        val p4 = json.decodeFromString(Playlist.serializer(), jsonDefault)
        assertEquals(0.0, p4.playCount, 0.001)
    }

    @Test
    fun testGeneralResponse() {
        val full = """{"code":200,"message":"success","msg":"ok"}"""
        val resp1 = json.decodeFromString(GeneralResponse.serializer(), full)
        assertEquals(200, resp1.code)
        assertEquals("success", resp1.message)

        val empty = """{}"""
        val resp2 = json.decodeFromString(GeneralResponse.serializer(), empty)
        assertEquals(200, resp2.code)
    }

    @Test
    fun testCookieProvider() {
        CookieProvider.clear()
        assertTrue(CookieProvider.get().isEmpty())

        CookieProvider.init(mapOf("MUSIC_U" to "token123", "os" to "android"))
        assertEquals("token123", CookieProvider.get()["MUSIC_U"])
        assertEquals("android", CookieProvider.get()["os"])

        CookieProvider.update {
            it["MUSIC_U"] = "new_token"
            it["__csrf"] = "csrf999"
        }
        assertEquals("new_token", CookieProvider.get()["MUSIC_U"])
        assertEquals("csrf999", CookieProvider.get()["__csrf"])
        assertEquals("android", CookieProvider.get()["os"])

        CookieProvider.clear()
        assertTrue(CookieProvider.get().isEmpty())
    }

    @Test
    fun testCryptoUtilsWeapiAndEapi() {
        val payload = """{"id":12345,"csrf_token":""}"""
        val weapiResult = CryptoUtils.weapi(payload)
        assertTrue(weapiResult.containsKey("params"))
        assertTrue(weapiResult.containsKey("encSecKey"))
        assertTrue(weapiResult["params"]!!.isNotEmpty())
        assertTrue(weapiResult["encSecKey"]!!.isNotEmpty())

        val eapiResult = CryptoUtils.eapi("/api/v6/playlist/detail", payload)
        assertTrue(eapiResult.containsKey("params"))
        assertTrue(eapiResult["params"]!!.isNotEmpty())
    }

    @Test
    fun testCheckApiResponseCode() {
        // 正常状态码 200..299
        HttpManager.checkApiResponseCode("""{"code":200,"data":{}}""")
        HttpManager.checkApiResponseCode("""{"code":204}""")

        // 无 code 端点（如 DailySongsResponse）绝不误伤抛出
        HttpManager.checkApiResponseCode("""{"dailySongs":[{"id":123}]}""")
        HttpManager.checkApiResponseCode("""{"result":[{"id":456}]}""")

        // 根元素为数组或非 JSON
        HttpManager.checkApiResponseCode("""[{"id":1},{"id":2}]""")
        HttpManager.checkApiResponseCode("""<html>502 Bad Gateway</html>""")

        // 错误状态码应准确抛出 NcmApiException
        try {
            HttpManager.checkApiResponseCode("""{"code":400,"message":"参数错误"}""")
            fail("Should throw NcmApiException on code 400")
        } catch (e: NcmApiException) {
            assertEquals(400, e.code)
            assertEquals("参数错误", e.message)
        }

        try {
            HttpManager.checkApiResponseCode("""{"code":502,"msg":"网关错误"}""")
            fail("Should throw NcmApiException on code 502")
        } catch (e: NcmApiException) {
            assertEquals(502, e.code)
            assertEquals("网关错误", e.message)
        }
    }

    @Test
    fun testMaskIfSensitive() {
        assertEquals("<masked>", HttpManager.maskIfSensitive("MUSIC_U", "token_val"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("password", "123456"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("csrf_token", "secret"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("deviceId", "device_xyz"))

        assertEquals("application/json", HttpManager.maskIfSensitive("Content-Type", "application/json"))
        assertEquals("Keep-Alive", HttpManager.maskIfSensitive("Connection", "Keep-Alive"))
    }

    @Test
    fun testGunzipToString() {
        val original = "Test string for Gzip compression and decompression! 单元测试"
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(bos).use {
            it.write(original.toByteArray(Charsets.UTF_8))
        }
        val decompressed = HttpManager.gunzipToString(bos.toByteArray())
        assertEquals(original, decompressed)
    }
}
