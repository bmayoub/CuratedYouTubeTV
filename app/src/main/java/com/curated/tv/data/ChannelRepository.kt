package com.curated.tv.data

import android.content.Context
import android.util.Log
import com.curated.tv.model.Channel
import com.curated.tv.model.ChannelFeed
import com.curated.tv.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ChannelRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ChannelRepository"
        const val DEFAULT_REMOTE_URL = "https://raw.githubusercontent.com/bmayoub/CuratedYouTubeTV/main/channels.json"
        private const val CONFIG_FILE_NAME = "channels.json"
        private const val CACHE_FILE_NAME = "channels_cache.json"

        // قنوات أساسية دائمة ومضمونة في الكود
        val DEFAULT_CHANNELS = listOf(
            Channel(
                name = "سكتش | Sketch",
                handle = "@worldbysketch",
                channelId = "UCr4xUqTzdvZ8U8PcEDL_YBg",
                url = "https://www.youtube.com/@worldbysketch",
                category = "معرفة ووثائقيات",
                enabled = true
            ),
            Channel(
                name = "قصص مدار | Qisas Madar",
                handle = "@Qisas_Madar",
                channelId = "UC6t0GlogFquiOyriK98hyBA",
                url = "https://www.youtube.com/@Qisas_Madar",
                category = "قصص وتاريخ",
                enabled = true
            )
        )
    }

    var lastErrorMessage: String = ""

    /**
     * تحميل القنوات: سحابياً من GitHub أولاً، ثم من الكاش المحلي، ثم من Assets، ثم القنوات الافتراضية
     */
    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        var jsonString: String? = null

        // 1. محاولة الجلب السحابي المباشر من GitHub
        try {
            val freshUrl = "$DEFAULT_REMOTE_URL?_t=${System.currentTimeMillis()}"
            Log.d(TAG, "Requesting channels from GitHub: $freshUrl")
            val request = Request.Builder()
                .url(freshUrl)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "CuratedYouTubeTV-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank() && body.contains("\"channels\"")) {
                        jsonString = body
                        // حفظ في الكاش الداخلي
                        try {
                            File(context.filesDir, CACHE_FILE_NAME).writeText(body)
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Remote sync failed: ${e.message}")
        }

        // 2. إذا فشل الجلب السحابي، فحص الكاش الداخلي
        if (jsonString == null) {
            try {
                val cachedFile = File(context.filesDir, CACHE_FILE_NAME)
                if (cachedFile.exists() && cachedFile.canRead()) {
                    jsonString = cachedFile.readText()
                }
            } catch (_: Throwable) {}
        }

        // 3. إذا لم يوجد كاش، فحص ملف Assets المدمج
        if (jsonString == null) {
            try {
                context.assets.open(CONFIG_FILE_NAME).bufferedReader().use {
                    jsonString = it.readText()
                }
            } catch (_: Throwable) {}
        }

        // 4. تحليل محتوى JSON
        if (!jsonString.isNullOrBlank()) {
            try {
                val root = JSONObject(jsonString)
                val array = root.getJSONArray("channels")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optBoolean("enabled", true)) {
                        channels.add(
                            Channel(
                                name = obj.getString("name"),
                                handle = obj.optString("handle", ""),
                                channelId = obj.getString("channel_id"),
                                url = obj.optString("url", ""),
                                category = obj.optString("category", "عام"),
                                enabled = true
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "JSON parse error: ${e.message}")
            }
        }

        // 5. إذا كانت القائمة فارغة لأي سبب، استخدام القنوات الافتراضية المضمونة
        if (channels.isEmpty()) {
            channels.addAll(DEFAULT_CHANNELS)
        }

        channels
    }

    /**
     * جلب الفيديوهات لكل القنوات
     */
    suspend fun fetchAllFeeds(): List<ChannelFeed> = withContext(Dispatchers.IO) {
        val channels = loadChannels()
        val feeds = mutableListOf<ChannelFeed>()
        val errorLogs = StringBuilder()

        for (channel in channels) {
            try {
                val videos = fetchChannelVideos(channel)
                if (videos.isNotEmpty()) {
                    feeds.add(ChannelFeed(channel = channel, videos = videos))
                } else {
                    errorLogs.append("لم يتم العثور على فيديوهات لـ ${channel.name} | ")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error fetching feed for ${channel.name}", e)
                errorLogs.append("${channel.name}: ${e.message} | ")
            }
        }

        if (feeds.isEmpty()) {
            lastErrorMessage = if (errorLogs.isNotEmpty()) errorLogs.toString() else "تعذر الاتصال بـ YouTube"
        } else {
            lastErrorMessage = ""
        }

        feeds
    }

    /**
     * جلب فيديوهات القناة عبر YouTube RSS
     */
    private fun fetchChannelVideos(channel: Channel): List<Video> {
        val videos = mutableListOf<Video>()
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=${channel.channelId}"

        val request = Request.Builder()
            .url(rssUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "YouTube RSS HTTP ${response.code} for ${channel.name}")
                return emptyList()
            }
            val xmlData = response.body?.string() ?: return emptyList()

            // تحليل بنية XML عبر Regex المباشر
            val entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL)
            val matcher = entryPattern.matcher(xmlData)

            val idPattern = Pattern.compile("<yt:videoId>(.*?)</yt:videoId>|<id>yt:video:(.*?)</id>", Pattern.DOTALL)
            val titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL)
            val publishedPattern = Pattern.compile("<published>(.*?)</published>", Pattern.DOTALL)
            val thumbPattern = Pattern.compile("<media:thumbnail[^>]+url=[\"']([^\"']+)[\"']", Pattern.DOTALL)
            val descPattern = Pattern.compile("<media:description>(.*?)</media:description>", Pattern.DOTALL)

            while (matcher.find()) {
                val entryXml = matcher.group(1) ?: continue

                // 1. معرف الفيديو
                val idMatcher = idPattern.matcher(entryXml)
                val videoId = if (idMatcher.find()) {
                    idMatcher.group(1) ?: idMatcher.group(2) ?: ""
                } else ""

                if (videoId.isBlank()) continue

                // 2. العنوان
                val titleMatcher = titlePattern.matcher(entryXml)
                val rawTitle = if (titleMatcher.find()) titleMatcher.group(1) ?: "" else ""
                val title = unescapeXml(rawTitle)

                // 3. تاريخ النشر
                val pubMatcher = publishedPattern.matcher(entryXml)
                val publishedRaw = if (pubMatcher.find()) pubMatcher.group(1) ?: "" else ""
                val publishedDate = publishedRaw.take(10)

                // 4. الصورة المصغرة
                val thumbMatcher = thumbPattern.matcher(entryXml)
                val thumbUrl = if (thumbMatcher.find()) thumbMatcher.group(1) ?: "" else ""
                val finalThumb = if (thumbUrl.isNotBlank()) thumbUrl else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                // 5. الوصف
                val descMatcher = descPattern.matcher(entryXml)
                val rawDesc = if (descMatcher.find()) descMatcher.group(1) ?: "" else ""
                val desc = unescapeXml(rawDesc)

                videos.add(
                    Video(
                        id = videoId.trim(),
                        title = title.ifBlank { "فيديو يوتيوب" },
                        channelName = channel.name,
                        channelId = channel.channelId,
                        publishedDate = publishedDate,
                        thumbnailUrl = finalThumb,
                        description = desc
                    )
                )
            }
        }
        return videos
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .trim()
    }
}
