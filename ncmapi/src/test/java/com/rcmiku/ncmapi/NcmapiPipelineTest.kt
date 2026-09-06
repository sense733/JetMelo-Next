package com.rcmiku.ncmapi

import com.rcmiku.ncmapi.model.FlexibleDoubleSerializer
import com.rcmiku.ncmapi.model.GeneralResponse
import com.rcmiku.ncmapi.model.NcmApiException
import com.rcmiku.ncmapi.model.NcmHttpException
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.CryptoUtils
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertFalse(CookieProvider.hasValidSession())
        CookieProvider.clearAll()
        assertTrue(CookieProvider.get().isEmpty())
    }

    @Test
    fun testCryptoUtilsWeapiAndEapi() {
        val payload = """{"id":12345,"csrf_token":""}"""
        val weapiResult = CryptoUtils.weapi(payload)
        val params = weapiResult["params"]!!
        val encSecKey = weapiResult["encSecKey"]!!
        assertTrue(params.isNotEmpty())
        // encSecKey 为 RSA-1024 NoPadding 输出：128 字节 → 定长 256 位十六进制
        assertTrue("encSecKey must be 256 hex chars", encSecKey.matches(Regex("^[0-9a-fA-F]{256}$")))

        // eapi 为确定性 ECB：密文可解密回原始 message 结构（黄金向量式回环验证）
        val eapiResult = CryptoUtils.eapi("/api/v6/playlist/detail", payload)
        val eapiParams = eapiResult["params"]!!
        assertTrue(eapiParams.isNotEmpty())
        assertTrue("eapi params must be hex", eapiParams.matches(Regex("^[0-9A-Fa-f]+$")))
        assertEquals(0, eapiParams.length % 16)
        val plain = CryptoUtils.eapiDecryptParams(eapiParams)
        assertEquals(payload, plain)
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

    @Test
    fun testHomepageBlockResponseDeserialization() {
        val jsonString = """
        {
            "code": 200,
            "data": {
                "cursor": "cursor_token_123",
                "hasMore": true,
                "blockCodeOrderList": ["HOMEPAGE_BANNER", "HOMEPAGE_BLOCK_PLAYLIST_RCMD"],
                "blocks": [
                    {
                        "blockCode": "HOMEPAGE_BANNER",
                        "showType": "BANNER",
                        "extInfo": {
                            "banners": [
                                {
                                    "pic": "https://p1.music.126.net/banner1.jpg",
                                    "bannerId": "1001",
                                    "targetId": 123456,
                                    "targetType": 1,
                                    "typeTitle": "新歌首发"
                                }
                            ]
                        }
                    },
                    {
                        "blockCode": "HOMEPAGE_BLOCK_PLAYLIST_RCMD",
                        "showType": "HOMEPAGE_SLIDE_PLAYLIST",
                        "uiElement": {
                            "subTitle": { "title": "推荐歌单" }
                        },
                        "creatives": [
                            {
                                "creativeType": "scroll_playlist",
                                "creativeId": "2001",
                                "uiElement": {
                                    "mainTitle": { "title": "今日专属雷达" },
                                    "image": { "imageUrl": "https://p1.music.126.net/radar.jpg" }
                                },
                                "resources": [
                                    {
                                        "resourceId": "3001",
                                        "resourceType": "playlist",
                                        "uiElement": {
                                            "mainTitle": { "title": "华语精选歌单" },
                                            "subTitle": { "title": "播放量 100万" },
                                            "image": { "imageUrl": "https://p1.music.126.net/cover.jpg" }
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
        """.trimIndent()

        val response = json.decodeFromString(com.rcmiku.ncmapi.model.HomepageBlockResponse.serializer(), jsonString)
        assertEquals(200, response.code)
        assertEquals("cursor_token_123", response.data.cursor)
        assertTrue(response.data.hasMore)
        assertEquals(2, response.data.blocks.size)

        // Banner 提取验证
        val bannerBlock = response.data.blocks[0]
        val banners = bannerBlock.extractBanners()
        assertEquals(1, banners.size)
        assertEquals("1001", banners[0].bannerId)
        assertEquals(123456L, banners[0].targetId)
        assertEquals("新歌首发", banners[0].typeTitle)

        // 歌单提取验证
        val playlistBlock = response.data.blocks[1]
        val playlists = playlistBlock.extractPlaylists()
        assertEquals(1, playlists.size)
        assertEquals(3001L, playlists[0].id)
        assertEquals("华语精选歌单", playlists[0].name)
        assertEquals("https://p1.music.126.net/cover.jpg", playlists[0].coverUrl)
        assertEquals("播放量 100万", playlists[0].playCountText)
    }

    @Test
    fun testCheckApiResponseCodeNestedBatch() {
        val batchJson = """
            {
                "code": 200,
                "/api/user/info": {"code": 200, "data": {}},
                "/api/playlist/detail": {"code": 301, "message": "need login"}
            }
        """.trimIndent()
        try {
            HttpManager.checkApiResponseCode(batchJson, endpoint = "https://music.163.com/eapi/batch")
            fail("Expected NcmApiException for nested batch error code")
        } catch (e: NcmApiException) {
            assertEquals(301, e.code)
            assertTrue(e.message.contains("batch[/api/playlist/detail] error code: 301"))
            assertEquals("https://music.163.com/eapi/batch", e.endpoint)
        }
    }

    @Test
    fun testMaskIfSensitiveEapiKeys() {
        assertEquals("<masked>", HttpManager.maskIfSensitive("jsessionid", "123456"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("music_u", "abcdef"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("osver", "14"))
        assertEquals("<masked>", HttpManager.maskIfSensitive("kaola", "xyz"))
        assertEquals("application/json", HttpManager.maskIfSensitive("Content-Type", "application/json"))
        assertEquals("keep-alive", HttpManager.maskIfSensitive("Connection", "keep-alive"))
    }

    @Test
    fun testEapiDelimiterCollision() {
        val payloadWithDelimiter = """{"comment":"hello-36cd479b6b5-world"}"""
        val encrypted = CryptoUtils.eapi("https://music.163.com/eapi/song/enhance/player/url/v1", payloadWithDelimiter)
        val paramsHex = checkNotNull(encrypted["params"]) { "Missing params" }
        val decryptedPayload = CryptoUtils.eapiDecryptParams(paramsHex)
        assertEquals(payloadWithDelimiter, decryptedPayload)
    }

    @Test
    fun testEapiDecryptInvalidHex() {
        try {
            CryptoUtils.eapiDecryptParams("")
            fail("Expected IllegalArgumentException for empty hex")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid EAPI params hex") == true)
        }

        try {
            CryptoUtils.eapiDecryptParams("ABC")
            fail("Expected IllegalArgumentException for odd length hex")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid EAPI params hex") == true)
        }

        try {
            CryptoUtils.eapiDecryptParams("ZZZZ")
            fail("Expected IllegalArgumentException for non-hex chars")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid EAPI params hex") == true)
        }
    }

    @Test
    fun testExceptionRetryContext() {
        val apiEx429 = NcmApiException(code = 429, endpoint = "/api/v1")
        assertTrue(apiEx429.retryable)
        assertEquals("/api/v1", apiEx429.endpoint)

        val apiEx503 = NcmApiException(code = 503, endpoint = "/api/v2")
        assertTrue(apiEx503.retryable)

        val apiEx400 = NcmApiException(code = 400)
        assertFalse(apiEx400.retryable)
        assertNull(apiEx400.endpoint)

        val httpEx502 = NcmHttpException(statusCode = 502, endpoint = "/api/http")
        assertTrue(httpEx502.retryable)
        assertEquals("/api/http", httpEx502.endpoint)

        val httpEx404 = NcmHttpException(statusCode = 404)
        assertFalse(httpEx404.retryable)
        assertNull(httpEx404.endpoint)
    }

    @Test
    fun testModelHelpers() {
        val successResp = GeneralResponse(code = 200)
        assertTrue(successResp.isSuccess)
        val failResp = GeneralResponse(code = 400)
        assertFalse(failResp.isSuccess)

        val pEmpty = Playlist(id = 1, name = "Empty")
        assertTrue(pEmpty.hasAlignedTracks())

        val pDetail = com.rcmiku.ncmapi.model.PlaylistDetailResponse()
        assertTrue(pDetail.hasAlignedTracksAndPrivileges())

        val recEmpty = com.rcmiku.ncmapi.model.RecordResponse()
        assertTrue(recEmpty.isEmpty)

        val suggestSongWithArtist = com.rcmiku.ncmapi.model.SuggestSong(
            id = 1,
            name = "Song1",
            artists = listOf(com.rcmiku.ncmapi.model.Artist(id = 10, name = "ArtistA"))
        )
        assertEquals("ArtistA", suggestSongWithArtist.artistNameOrNull)

        val suggestSongNoArtist = com.rcmiku.ncmapi.model.SuggestSong(id = 2, name = "Song2")
        assertNull(suggestSongNoArtist.artistNameOrNull)

        val suggestAlbumWithArtist = com.rcmiku.ncmapi.model.SuggestAlbum(
            id = 1,
            name = "Album1",
            artist = com.rcmiku.ncmapi.model.Artist(id = 10, name = "ArtistA")
        )
        assertEquals("ArtistA", suggestAlbumWithArtist.artistNameOrNull)

        val suggestAlbumNoArtist = com.rcmiku.ncmapi.model.SuggestAlbum(id = 2, name = "Album2")
        assertNull(suggestAlbumNoArtist.artistNameOrNull)
    }

    @Test
    fun testClearSession() {
        HttpManager.clearSession()
    }

    @Test
    fun testFavoriteSongResponseDeserialization() {
        val likelistJson = """{"ids":[12345,67890],"checkPoint":1503019930000,"code":200}"""
        val resp = json.decodeFromString(com.rcmiku.ncmapi.model.FavoriteSongResponse.serializer(), likelistJson)
        assertEquals(200, resp.code)
        assertEquals(listOf(12345L, 67890L), resp.ids)
        assertEquals(1503019930000L, resp.checkPoint)

        val favPlaylistJson = """{"code":200,"data":{"id":999,"name":"喜欢的音乐","trackCount":50}}"""
        val favResp = json.decodeFromString(com.rcmiku.ncmapi.model.FavoriteSongResponse.serializer(), favPlaylistJson)
        assertEquals(200, favResp.code)
        assertEquals(999L, favResp.data.id)
        assertEquals(50, favResp.data.trackCount)
        assertTrue(favResp.ids.isEmpty())
    }
}
