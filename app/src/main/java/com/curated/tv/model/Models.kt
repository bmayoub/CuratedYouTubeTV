package com.curated.tv.model

data class Channel(
    val name: String,
    val handle: String,
    val channelId: String,
    val url: String,
    val category: String = "عام",
    val enabled: Boolean = true
)

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedDate: String,
    val thumbnailUrl: String,
    val description: String = ""
)

data class ChannelFeed(
    val channel: Channel,
    val videos: List<Video>
)

