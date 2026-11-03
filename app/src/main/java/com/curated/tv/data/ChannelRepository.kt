package com.curated.tv.data

import android.content.Context
import android.os.Environment
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ChannelRepository"
        private const val CONFIG_FILE_NAME = "channels.json"
        private const val CACHE_FILE_NAME = "channels_cache.json"
    }

    /**
     * قراءة رابط التحديث السحابي من الملف المحلي أو الافتراضي
     */
    private fun getInitialConfig(): Pair<String, String?> {
        // 1. فحص الملف المؤقت السابق
        val cachedFile = File(context.filesDir, CACHE_FILE_NAME)
        if (cachedFile.exists() && cachedFile.canRead()) {
            try {
                val content = cachedFile.readText()
                val root = JSONObject(content)
                val remoteUrl = root.optString("remote_config_url", "").trim()
                return Pair(content, remoteUrl)
            } catch (_: Exception) {}
        }

        // 2. فحص مجلد التنزيلات
        val externalFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            CONFIG_FILE_NAME
        )
        if (externalFile.exists() && externalFile.canRead()) {
            try {
                val content = externalFile.readText()
                val root = JSONObject(content)
                val remoteUrl = root.optString("remote_config_url", "").trim()
                return Pair(content, remoteUrl)
            } catch (_: Exception) {}
        }

        // 3. قراءة الملف المدمج في assets
        return try {
            val content = context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
            val root = JSONObject(content)
            val remoteUrl = root.optString("remote_config_url", "").trim()
            Pair(content, remoteUrl)
        } catch (e: Exception) {
            Pair("", null)
        }
    }

    /**
     * قراءة ملف القنوات (سحابياً من GitHub أولاً مع كاش محلي واحتياطي أوفلاين)
     */
    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        var finalJsonString: String? = null

        val (initialJson, configuredRemoteUrl) = getInitialConfig()

        // 1. محاولة الجلب السحابي المباشر من GitHub إذا كان الرابط محدداً
        if (!configuredRemoteUrl.isNullOrEmpty() && configuredRemoteUrl.startsWith("http")) {
            try {
                val separator = if (configuredRemoteUrl.contains("?")) "&" else "?"
                val freshUrl = "$configuredRemoteUrl${separator}_t=${System.currentTimeMillis()}"

                Log.d(TAG, "Fetching latest channels from GitHub: $freshUrl")
                val request = Request.Builder()
                    .url(freshUrl)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && body.contains("\"channels\"")) {
                            finalJsonString = body
                            try {
                                File(context.filesDir, CACHE_FILE_NAME).writeText(body)
                                Log.d(TAG, "Successfully synced and cached channels from GitHub.")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to write local cache: ${e.message}")
                            }
                        }
                    } else {
                        Log.w(TAG, "GitHub remote fetch returned HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync channels from GitHub, using local fallback: ${e.message}")
            }
        }

        // 2. إذا لم ينجح الجلب السحابي، استخدام الكاش أو الملف الأولي
        if (finalJsonString == null) {
            finalJsonString = initialJson.ifEmpty { null }
        }

        // 3. معالجة محتوى الـ JSON
        try {
            finalJsonString?.let { content ->
                val root = JSONObject(content)
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
                                enabled = obj.optBoolean("enabled", true)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing channels json", e)
        }

        // قنوات أمان افتراضية في حال تعذر قراءة أي ملف
        if (channels.isEmpty()) {
            channels.add(
                Channel(
                    name = "سكتش | Sketch",
                    handle = "@worldbysketch",
                    channelId = "UCr4xUqTzdvZ8U8PcEDL_YBg",
                    url = "https://www.youtube.com/@worldbysketch",
                    category = "معرفة ووثائقيات"
                )
            )
            channels.add(
                Channel(
                    name = "قصص مدار | Qisas Madar",
                    handle = "@Qisas_Madar",
                    channelId = "UC6t0GlogFquiOyriK98hyBA",
                    url = "https://www.youtube.com/@Qisas_Madar",
                    category = "قصص وتاريخ"
                )
            )
        }

        channels
    }

    /**
     * جلب فيديوهات جميع القنوات عبر YouTube RSS Feed
     */
    suspend fun fetchAllFeeds(): List<ChannelFeed> = withContext(Dispatchers.IO) {
        val channels = loadChannels()
        Log.d(TAG, "Loaded channels count: ${channels.size}")
        val feeds = mutableListOf<ChannelFeed>()

        for (channel in channels) {
            try {
                val videos = fetchChannelVideos(channel)
                Log.d(TAG, "Fetched ${videos.size} videos for channel: ${channel.name}")
                if (videos.isNotEmpty()) {
                    feeds.add(ChannelFeed(channel = channel, videos = videos))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching feed for ${channel.name}", e)
            }
        }
        feeds
    }

    /**
     * جلب الفيديوهات لقناة معينة عبر YouTube RSS وتحليلها بدقة عبر Regex المقاوم لاختلافات XML
     */
    private fun fetchChannelVideos(channel: Channel): List<Video> {
        val videos = mutableListOf<Video>()
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=${channel.channelId}"

        val request = Request.Builder()
            .url(rssUrl)
            .header("User-Agent", "Mozilla/5.0 (Android TV; Linux; Google TV)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "RSS fetch HTTP error ${response.code} for ${channel.name}")
                    return emptyList()
                }
                val xmlData = response.body?.string() ?: return emptyList()

                // استخراج جميع كتل <entry>...</entry>
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

                    // 2. عنوان الفيديو
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
        } catch (e: Exception) {
            Log.e(TAG, "Exception parsing RSS for channel ${channel.name}: ${e.message}", e)
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
