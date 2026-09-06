package com.rcmiku.ncmapi.model

import android.util.Log
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HomepageBlockResponse(
    val code: Int = 200,
    val message: String? = null,
    val data: HomepageBlockData = HomepageBlockData()
)

@Serializable
data class HomepageBlockData(
    val cursor: String? = null,
    val hasMore: Boolean = false,
    val blockCodeOrderList: List<String> = emptyList(),
    val blocks: List<HomepageBlock> = emptyList()
)

@Serializable
data class HomepageBlock(
    val blockCode: String = "",
    val showType: String = "",
    val action: String? = null,
    val actionType: String? = null,
    val uiElement: BlockUiElement? = null,
    val creatives: List<BlockCreative> = emptyList(),
    val extInfo: JsonElement? = null
) {
    fun extractBanners(): List<HomepageBanner> {
        val element = extInfo ?: return emptyList()
        return runCatching {
            json.decodeFromJsonElement(HomepageBannerExtInfo.serializer(), element).banners
        }.getOrElse { e ->
            if (HttpManager.debugLogEnabled) {
                Log.w("HomepageBlock", "extractBanners failed: element=$element", e)
            }
            emptyList()
        }
    }

    fun extractPlaylists(): List<SimplifiedBlockPlaylist> {
        val result = mutableListOf<SimplifiedBlockPlaylist>()
        for (creative in creatives) {
            if (creative.resources.isNotEmpty()) {
                for (res in creative.resources) {
                    val id = res.resourceId.toLongOrNull() ?: continue
                    val name = res.uiElement?.mainTitle?.title
                        ?: creative.uiElement?.mainTitle?.title
                        ?: ""
                    val cover = res.uiElement?.image?.imageUrl
                        ?: creative.uiElement?.image?.imageUrl
                        ?: ""
                    val count = res.uiElement?.subTitle?.title
                        ?: creative.uiElement?.subTitle?.title
                    result.add(
                        SimplifiedBlockPlaylist(
                            id = id,
                            name = name,
                            coverUrl = cover,
                            playCountText = count
                        )
                    )
                }
            } else {
                val id = creative.creativeId.toLongOrNull() ?: continue
                val name = creative.uiElement?.mainTitle?.title ?: ""
                val cover = creative.uiElement?.image?.imageUrl ?: ""
                val count = creative.uiElement?.subTitle?.title
                result.add(
                    SimplifiedBlockPlaylist(
                        id = id,
                        name = name,
                        coverUrl = cover,
                        playCountText = count
                    )
                )
            }
        }
        return result
    }
}

@Serializable
data class BlockCreative(
    val creativeType: String = "",
    val creativeId: String = "",
    val action: String? = null,
    val actionType: String? = null,
    val uiElement: BlockUiElement? = null,
    val resources: List<BlockResource> = emptyList(),
    val position: Int = 0
)

@Serializable
data class BlockResource(
    val uiElement: BlockUiElement? = null,
    val resourceType: String = "",
    val resourceId: String = "",
    val action: String? = null,
    val actionType: String? = null,
    val resourceExtInfo: JsonElement? = null
)

@Serializable
data class BlockUiElement(
    val mainTitle: BlockText? = null,
    val subTitle: BlockText? = null,
    val image: BlockImage? = null,
    val labelTexts: List<String> = emptyList(),
    val button: BlockButton? = null,
    val rcmdShowType: String? = null
)

@Serializable
data class BlockText(
    val title: String = ""
)

@Serializable
data class BlockImage(
    val imageUrl: String = ""
)

@Serializable
data class BlockButton(
    val action: String? = null,
    val actionType: String? = null,
    val text: String = "",
    val iconUrl: String? = null
)

@Serializable
data class HomepageBanner(
    val pic: String = "",
    val bannerId: String = "",
    val url: String? = null,
    val targetId: Long = 0,
    val targetType: Int = 0,
    val titleColor: String? = null,
    val typeTitle: String? = null
)

@Serializable
data class HomepageBannerExtInfo(
    val banners: List<HomepageBanner> = emptyList()
)

data class SimplifiedBlockPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val playCountText: String? = null
)
