package com.shiftsaver.model

data class DownloadRequest(
    val url: String,
    val quality: String = "best"
)

data class DownloadResponse(
    val success: Boolean,
    val filename: String?,
    val filepath: String?,
    val filesize: Long?,
    val title: String?,
    val thumbnail: String?,
    val platform: String?,
    val error: String?
)

data class StatusResponse(
    val status: String,
    val version: String
)

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val platform: Platform,
    val state: DownloadState,
    val filepath: String? = null,
    val thumbnail: String? = null,
    val filesize: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadState {
    QUEUED, DOWNLOADING, DONE, ERROR
}

enum class Platform(val label: String) {
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    UNKNOWN("Unknown");

    companion object {
        fun fromUrl(url: String): Platform {
            return when {
                url.contains("youtube.com") || url.contains("youtu.be") -> YOUTUBE
                url.contains("tiktok.com") -> TIKTOK
                url.contains("instagram.com") -> INSTAGRAM
                else -> UNKNOWN
            }
        }
    }
}
