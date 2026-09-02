package com.rcmiku.ncmapi.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.doubleOrNull?.let { return it }
                val str = element.content
                str.toDoubleOrNull()?.let { return it }
            }
            return 0.0
        }
        return decoder.decodeDouble()
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }
}

@Serializable
data class GeneralResponse(
    val code: Int = 200,
    val message: String? = null,
    val msg: String? = null
)

@Serializable
data class Song(
    val id: Long,
    val name: String,
    val ar: List<Artist>,
    val al: SongAlbum = SongAlbum(id = 0, name = ""),
    val dt: Long = 0, // Duration
    val fee: Int = 0,
    val pop: Double = 0.0,
    val no: Int = 0,
    val rt: String? = null,
    val mst: Int = 0,
    val cp: Int = 0,
    val crbt: String? = null,
    val cf: String? = null,
    val mv: Long = 0,
) {
    val artists: List<Artist> get() = ar
}

@Serializable
data class Artist(
    val id: Long,
    val name: String = "",
    val picUrl: String? = null,
    val alias: List<String> = emptyList(),
    val albumSize: Int = 0,
    val picId: Long = 0,
    val img1v1Url: String? = null,
    val img1v1: Long = 0,
    val trans: String? = null
)

@Serializable
data class SearchArtist(
    val id: Long,
    val name: String,
    val picUrl: String? = null,
    val alias: List<String> = emptyList(),
    val albumSize: Int = 0,
    val picId: Long = 0,
    val img1v1Url: String? = null,
    val img1v1: Long = 0,
    val trans: String? = null
)

@Serializable
data class Album(
    val id: Long,
    val name: String = "",
    val picUrl: String = "",
    val pic: Long = 0,
    val picId: Long = 0,
    val publishTime: Long = 0,
    val company: String? = null,
    val size: Int = 0,
    val artist: Artist = Artist(id = 0, name = ""),
    val artists: List<Artist> = emptyList(),
    val description: String? = null,
    val type: String? = null,
    val blurPicUrl: String? = null
)

@Serializable
data class SongAlbum(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val pic: Long = 0
)

@Serializable
data class SubAlbum(
    val id: Long,
    val name: String,
    val picUrl: String,
    val artists: List<Artist> = emptyList(),
    val size: Int = 0
)

@Serializable
data class PlaylistV1(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val trackCount: Int,
    val playCount: Long,
    val containsTracks: Boolean = false
)

@Serializable
data class VoiceBaseInfo(
    val id: Long,
    val name: String,
    val picUrl: String,
    val programCount: Int
)

@Serializable
data class CloudSong(
    val simpleSong: Song,
    val songName: String = "",
    val artist: String = "",
    val album: String = "",
    val picUrl: String = ""
)

@Serializable
data class PrivateCloud(
    val id: Long = 0,
    val userId: Long = 0,
    val songId: Long = 0,
    val fileName: String = "",
    val fileSize: Long = 0,
    val addTime: Long = 0,
    val bitrate: Int = 0
)

@Serializable
data class Radio(
    val id: Long,
    val name: String,
    val dj: Artist? = null,
    val copywriter: String? = null,
    val picUrl: String? = null,
    val createTime: Long = 0,
    val categoryId: Int = 0,
    val category: String? = null,
    val secondCategory: String? = null,
    val radioFeeType: Int = 0,
    val feeScope: Int = 0,
    val programCount: Int = 0,
    val subCount: Int = 0,
    val playCount: Int = 0,
    val lastProgramCreateTime: Long = 0,
    val lastProgramName: String? = null,
    val lastProgramId: Long = 0,
    val rcmdText: String? = null,
    val subed: Boolean = false,
    val originalPrice: Int = 0,
    val price: Int = 0,
    val discountPrice: Int? = null,
    val lastProgram: String? = null,
    val mainSong: Song
) {
    val coverUrl: String get() = picUrl ?: ""
}

@Serializable
data class AlbumSublistResponse(
    val data: List<SubAlbum>,
    val hasMore: Boolean,
    val count: Int
)

@Serializable
data class ArtistAlbumResponse(
    val hotAlbums: List<Album>,
    val more: Boolean
)

@Serializable
data class CloudSongResponse(
    val data: List<CloudSong> = emptyList(),
    val hasMore: Boolean = false,
    val count: Int = 0,
    val upgradeCount: Int = 0
)

@Serializable
data class ProgramRadioResponse(
    val data: ProgramRadioData = ProgramRadioData(),
    val code: Int = 200
)

@Serializable
data class ProgramRadioData(
    val programs: List<Radio> = emptyList(),
    val more: Boolean = false,
    val count: Int = 0
)

@Serializable
data class SearchResponse(
    val code: Int = 200,
    val message: String? = null,
    val data: SearchData = SearchData()
)

@Serializable
data class PlaylistTerminalSearchResponse(
    val code: Int = 200,
    val message: String? = null,
    val data: PlaylistTerminalSearchData = PlaylistTerminalSearchData()
)

@Serializable
data class PlaylistTerminalSearchData(
    val resources: List<Playlist> = emptyList(),
    val more: Boolean = false,
    val totalCount: Int = 0,
    val moreText: String? = null
)

@Serializable
data class SearchData(
    val resources: List<SearchResources> = emptyList(),
    val more: Boolean = false,
    val totalCount: Int = 0,
    val moreText: String? = null
)

@Serializable
data class SearchResources(
    val resourceType: String = "",
    val resourceId: String = "",
    val baseInfo: VoiceBaseInfo? = null,
    val song: Song? = null,
    val album: Album? = null,
    val artist: Artist? = null,
    val playlist: Playlist? = null
)

fun SearchResources.toAlbumList(): Album? = album

fun SearchResources.toPlaylist(): Playlist? = playlist

fun SearchResources.toSearchArtist(): SearchArtist? = artist?.let { a ->
    val resolvedId = if (a.id != 0L) {
        a.id
    } else {
        resourceId.toLongOrNull() ?: 0L
    }
    SearchArtist(
        id = resolvedId,
        name = a.name ?: "",
        picUrl = a.picUrl,
        alias = a.alias,
        albumSize = a.albumSize,
        picId = a.picId,
        img1v1Url = a.img1v1Url,
        img1v1 = a.img1v1,
        trans = a.trans
    )
}

@Serializable
data class UserPlaylistV1Response(
    val playlist: List<PlaylistV1>,
    val code: Int
)

@Serializable
data class UserPlaylistData(
    val playlist: List<Playlist> = emptyList()
)

@Serializable
data class ArtistVideoResponse(
    val code: Int,
    val message: String? = null,
    val data: ArtistVideoData? = null
)

@Serializable
data class ArtistVideoData(
    val records: List<ArtistVideoRecord> = emptyList(),
    val page: ArtistVideoPage? = null
)

@Serializable
data class ArtistVideoPage(
    val size: Int = 0,
    val cursor: String? = null,
    val more: Boolean = false
)

@Serializable
data class ArtistVideoRecord(
    val id: String,
    val type: Int = 0,
    val position: Int? = null,
    val resource: ArtistVideoResource? = null
)

@Serializable
data class ArtistVideoResource(
    val mlogBaseData: MlogBaseData? = null,
    val mlogExtVO: MlogExtVO? = null,
    val status: Int? = null,
    val shareUrl: String? = null
)

@Serializable
data class MlogBaseData(
    val id: String,
    val userId: Long? = null,
    val type: Int? = null,
    val text: String? = null,
    val desc: String? = null,
    val pubTime: Long? = null,
    val coverUrl: String? = null,
    val duration: Long? = null,
    val threadId: String? = null,
    val video: MlogVideoInfo? = null,
    val videos: List<MlogVideo> = emptyList()
)

@Serializable
data class MlogVideoInfo(
    val videoKey: String? = null,
    val duration: Long? = null,
    val coverUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val urlInfo: MlogVideoUrlInfo? = null,
    val urlInfos: List<MlogVideoUrlInfo> = emptyList()
)

@Serializable
data class MlogVideoUrlInfo(
    val id: String? = null,
    val url: String? = null,
    val size: Long? = null,
    val r: Int? = null,
    val validityTime: Int? = null,
    val resolution: Int? = null
)

@Serializable
data class MlogVideo(
    val tag: String? = null,
    val url: String? = null,
    val duration: Double? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val container: String? = null
)

@Serializable
data class MlogExtVO(
    val likedCount: Long? = null,
    val commentCount: Long? = null,
    val playCount: Long? = null,
    val shareCount: Long? = null,
    val liked: Boolean? = null,
    val artistName: String? = null,
    val artists: List<MlogArtist> = emptyList()
)

@Serializable
data class MlogArtist(
    val id: Long,
    val name: String? = null,
    val img1v1Url: String? = null
)

@Serializable
data class MvUrlResponse(
    val code: Int,
    val data: MvUrlData? = null
)

@Serializable
data class MvUrlData(
    val id: Long? = null,
    val url: String? = null,
    val r: Int? = null,
    val size: Long? = null,
    val md5: String? = null
)

@Serializable
data class UserPlaylistResponse(
    val data: UserPlaylistData = UserPlaylistData(),
    val code: Int = 200,
    val playlist: List<Playlist> = emptyList()
)

@Serializable
data class AlbumDetailResponse(
    val album: Album = Album(id = 0),
    val songs: List<Song> = emptyList()
)

@Serializable
data class AlbumInfoResponse(
    val album: Album = Album(id = 0),
    val songs: List<Song> = emptyList(),
    val isSub: Boolean = false
)

@Serializable
data class AlbumDetailDynamicResponse(
    val code: Int = 200,
    val isSub: Boolean = false
)

@Serializable
data class ArtistHeadInfoResponse(
    val data: ArtistHeadInfoData = ArtistHeadInfoData()
)

@Serializable
data class ArtistHeadInfoData(
    val artist: ArtistProfile = ArtistProfile()
)

@Serializable
data class ArtistProfile(
    val name: String = "",
    val cover: String? = null,
    val briefDesc: String? = null
)

@Serializable
data class ArtistTopSong(
    val songs: List<Song> = emptyList(),
    val more: Boolean = false
)

@Serializable
data class ArtistDescResponse(
    val code: Int = 200,
    val briefDesc: String? = null,
    val introduction: List<ArtistIntroductionItem> = emptyList()
)

@Serializable
data class ArtistIntroductionItem(
    val ti: String? = null,
    val txt: String? = null
)

@Serializable
data class NewAlbumResponse(
    val albums: List<Album> = emptyList()
) {
    @Deprecated("Misleading getter, returns albums instead of distinct week data", ReplaceWith("albums"))
    val weekData: List<Album> get() = albums

    @Deprecated("Misleading getter, returns albums instead of distinct month data", ReplaceWith("albums"))
    val monthData: List<Album> get() = albums
}

@Serializable
data class TopListResponse(
    val list: List<Playlist> = emptyList()
)

@Serializable
data class DailySongsResponse(
    val data: DailySongsData = DailySongsData()
)

@Serializable
data class DailySongsData(
    val dailySongs: List<Song> = emptyList()
)

@Serializable
data class PersonalizedPlaylistResponse(
    val result: List<Playlist> = emptyList()
)

@Serializable
data class RecommendPlaylistResponse(
    val recommend: List<Playlist> = emptyList()
)

@Serializable
data class FavoriteSongResponse(
    val data: FavoriteSongData = FavoriteSongData(),
    val code: Int = 200,
    val ids: List<Long> = emptyList()
)

@Serializable
data class FavoriteSongData(
    val id: Long = 0,
    val name: String? = null,
    val coverImgUrl: String? = null,
    val trackCount: Int = 0,
    val userId: Long = 0
)

@Serializable
data class UserInfoBatch(
    val account: UserAccount = UserAccount(),
    val level: UserLevel = UserLevel()
) {
    val profile: UserProfile get() = account.profile
}

@Serializable
data class UserAccount(
    val profile: UserProfile = UserProfile()
)

@Serializable
data class UserProfile(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
    val vipType: Int = 0
)

@Serializable
data class UserLevelData(
    val level: Int = 0
)

@Serializable
data class UserLevel(
    val data: UserLevelData = UserLevelData()
)

@Serializable
data class Account(
    val id: Long = 0,
    val userName: String? = null,
    val vipType: Int = 0
)

@Serializable
data class User(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = ""
)

@Serializable
data class UserDetailResponse(
    val code: Int = 200,
    val level: Int = 0,
    val listenSongs: Int = 0,
    val profile: Profile? = null
)

@Serializable
data class Profile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
    val signature: String? = null,
    val userName: String? = null,
    val vipType: Int = 0,
    val backgroundUrl: String? = null
)

@Serializable
data class LyricResponse(
    val lrc: LyricData? = null,
    val klyric: LyricData? = null,
    val tlyric: LyricData? = null,
    val romalrc: LyricData? = null,
    val ytlrc: LyricData? = null,
    val yromalrc: LyricData? = null
)

@Serializable
data class LyricData(
    val version: Int = 0,
    val lyric: String = ""
)

@Serializable
data class PlaylistDetailResponse(
    val playlist: Playlist = Playlist(id = 0, name = ""),
    val privileges: List<Privilege> = emptyList()
)

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val coverImgUrl: String? = null,
    val picUrl: String? = null,
    val creator: User? = null,
    val trackCount: Int = 0,
    val trackIds: List<TrackId> = emptyList(),
    val tracks: List<Song> = emptyList(),
    @Serializable(with = FlexibleDoubleSerializer::class)
    val playCount: Double = 0.0,
    val userId: Long = 0,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val createTime: Long = 0,
    val updateTime: Long = 0,
    val trackUpdateTime: Long = 0,
    val subscribedCount: Long = 0,
    val shareCount: Long = 0,
    val commentCount: Long = 0,
    val subscribed: Boolean = false
) {
    val cover: String get() = coverImgUrl ?: picUrl ?: ""
    val pic: String get() = picUrl ?: ""
}

@Serializable
data class TrackId(
    val id: Long
)

@Serializable
data class Privilege(
    val id: Long = 0,
    val fee: Int = 0,
    val payed: Int = 0,
    val st: Int = 0,
    val pl: Int = 0,
    val dl: Int = 0,
    val sp: Int = 0,
    val cp: Int = 0,
    val subp: Int = 0,
    val cs: Boolean = false,
    val maxbr: Int = 0,
    val fl: Int = 0,
    val toast: Boolean = false,
    val flag: Int = 0
)

@Serializable
data class PlaylistInfoResponse(
    val playlist: Playlist,
    val subscribed: Boolean = playlist.subscribed
)

@Serializable
data class RadioInfoResponse(
    val data: RadioInfoData = RadioInfoData()
)

@Serializable
data class RadioInfoData(
    val picUrl: String? = null,
    val name: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class)
    val playCount: Double = 0.0,
    val desc: String? = null
)

@Serializable
data class RecordResponse(
    val weekData: List<PlayRecord> = emptyList(),
    val allData: List<PlayRecord> = emptyList()
)

@Serializable
data class PlayRecord(
    val playCount: Int,
    val score: Int,
    val song: Song
)

@Serializable
data class SearchSuggestKeywordResponse(
    val code: Int = 200,
    val result: SearchSuggestKeywordResult? = null
)

@Serializable
data class SearchSuggestKeywordResult(
    val allMatch: List<SearchSuggestMatch> = emptyList()
)

@Serializable
data class SearchSuggestMatch(
    val keyword: String,
    val type: Int = 0,
    val alg: String = "",
    val lastKeyword: String = ""
)

@Serializable
data class SearchHotResponse(
    val code: Int = 200,
    val result: SearchHotResult? = null
)

@Serializable
data class SearchHotResult(
    val hots: List<SearchHotItem> = emptyList()
)

@Serializable
data class SearchHotItem(
    val first: String
)

@Serializable
data class WebSearchSuggestResponse(
    val code: Int = 200,
    val result: WebSearchSuggestResult? = null
)

@Serializable
data class WebSearchSuggestResult(
    val allMatch: List<SearchSuggestMatch> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val songs: List<SuggestSong> = emptyList(),
    val albums: List<SuggestAlbum> = emptyList(),
    val order: List<String> = emptyList()
)

@Serializable
data class SuggestSong(
    val id: Long,
    val name: String,
    val artists: List<Artist> = emptyList(),
    val album: SongAlbum? = null,
    val duration: Long = 0
) {
    val artistName: String get() = artists.joinToString("/") { it.name }
}

@Serializable
data class SuggestAlbum(
    val id: Long,
    val name: String,
    val artist: Artist? = null
) {
    val artistName: String get() = artist?.name ?: ""
}

