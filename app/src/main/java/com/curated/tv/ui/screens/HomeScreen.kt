package com.curated.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.curated.tv.model.Category
import com.curated.tv.model.ChannelFeed
import com.curated.tv.model.Video
import com.curated.tv.ui.components.VideoCard
import com.curated.tv.ui.theme.TextPrimary
import com.curated.tv.ui.theme.TextSecondary
import com.curated.tv.ui.theme.YoutubeBlack
import com.curated.tv.ui.theme.YoutubeDarkGray
import com.curated.tv.ui.theme.YoutubeRed

@Composable
fun HomeScreen(
    feeds: List<ChannelFeed>,
    isLoading: Boolean,
    errorMessage: String = "",
    onVideoSelected: (Video) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(YoutubeBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = YoutubeRed)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "جاري الاتصال وجلب الفيديوهات من YouTube...",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        }
        return
    }

    if (feeds.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(YoutubeBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "تعذر تحميل فيديوهات القنوات",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = "تفاصيل الخطأ: $errorMessage",
                        color = YoutubeRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = "يرجى التأكد من اتصال الشاشة بالإنترنت ثم الضغط على زر إعادة المحاولة",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = YoutubeRed,
                        focusedContainerColor = YoutubeRed
                    )
                ) {
                    Text(text = "🔄 إعادة المحاولة الآن", color = TextPrimary, fontSize = 16.sp)
                }
            }
        }
        return
    }

    var selectedCategory by remember { mutableStateOf(Category.ALL) }
    var shuffleSeed by remember { mutableIntStateOf(0) }

    // جميع الفيديوهات المتاحة من كل القنوات
    val allVideos = remember(feeds) { feeds.flatMap { it.videos } }

    // أحدث الفيديوهات مرتبة زمنياً
    val allRecentVideos = remember(allVideos) {
        allVideos.sortedByDescending { it.publishedDate }.take(20)
    }

    // فيديوهات الكوكتيل العشوائي (يتم إعادة خلطها عند تغيير shuffleSeed)
    val randomVideos = remember(allVideos, shuffleSeed) {
        allVideos.shuffled().take(30)
    }

    // خريطة تصنيفات القنوات
    val channelCategoryMap = remember(feeds) {
        feeds.associate { it.channel.channelId to it.channel.category }
    }

    // فيديوهات التصنيف المحدد
    val categoryVideos = remember(allVideos, selectedCategory) {
        if (selectedCategory.id == "all" || selectedCategory.id == "random") {
            emptyList()
        } else {
            allVideos.filter { video ->
                selectedCategory.matches(video, channelCategoryMap[video.channelId] ?: "")
            }
        }
    }

    // القنوات التابعة للتصنيف المحدد
    val matchingFeeds = remember(feeds, selectedCategory) {
        if (selectedCategory.id == "all" || selectedCategory.id == "random") {
            feeds
        } else {
            feeds.filter { feed ->
                selectedCategory.matches(
                    Video("", "", "", feed.channel.channelId, "", ""),
                    feed.channel.category
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(YoutubeBlack),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // 1. شريط التصنيفات العلوي التفاعلي (Chips Row)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(Category.DEFAULT_CATEGORIES, key = { it.id }) { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }
        }

        // 2. المحتوى بناءً على التصنيف المختار
        when (selectedCategory.id) {
            "all" -> {
                // صف أحدث الفيديوهات
                if (allRecentVideos.isNotEmpty()) {
                    item {
                        ChannelSection(
                            title = "🔥 أحدث الفيديوهات (من كل القنوات المعتمدة)",
                            subtitle = "أحدث ما تم نشره في قنواتك المختارة",
                            videos = allRecentVideos,
                            onVideoSelected = onVideoSelected
                        )
                    }
                }

                // صف الكوكتيل العشوائي
                if (randomVideos.isNotEmpty()) {
                    item {
                        ChannelSection(
                            title = "🎲 كوكتيل عشوائي (اكتشف من قنواتك)",
                            subtitle = "فيديوهات مختارة عشوائياً • تتجدد مع كل تحديث",
                            videos = randomVideos.take(15),
                            onVideoSelected = onVideoSelected
                        )
                    }
                }

                // صفوف كل القنوات الفردية
                items(feeds, key = { it.channel.channelId }) { feed ->
                    ChannelSection(
                        title = "📺 ${feed.channel.name}",
                        subtitle = "${feed.channel.category} • ${feed.videos.size} فيديو متوفر",
                        videos = feed.videos,
                        onVideoSelected = onVideoSelected
                    )
                }
            }

            "random" -> {
                // رأس صفحة الكوكتيل العشوائي وزر إعادة الخلط
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎲 كوكتيل الفيديوهات العشوائية",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مجموعة مختارة عشوائياً من جميع القنوات المشترك بها",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { shuffleSeed++ },
                            shape = ButtonDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.colors(
                                containerColor = YoutubeDarkGray,
                                focusedContainerColor = YoutubeRed
                            )
                        ) {
                            Text(text = "🔄 خلط جديد", color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // صفوف الفيديوهات العشوائية
                if (randomVideos.isNotEmpty()) {
                    item {
                        ChannelSection(
                            title = "✨ باقة الاكتشاف (المجموعة الأولى)",
                            subtitle = "تشكيلة منوعة من مختلف المجالات",
                            videos = randomVideos.take(15),
                            onVideoSelected = onVideoSelected
                        )
                    }
                    if (randomVideos.size > 15) {
                        item {
                            ChannelSection(
                                title = "🌟 باقة الاكتشاف (المجموعة الثانية)",
                                subtitle = "المزيد من الاقتراحات المنوعة",
                                videos = randomVideos.drop(15),
                                onVideoSelected = onVideoSelected
                            )
                        }
                    }
                }
            }

            else -> {
                // تصنيف محدد (فضاء، طبيعة، فلسفة، تاريخ، علوم وتكنولوجيا...)
                if (categoryVideos.isNotEmpty()) {
                    item {
                        ChannelSection(
                            title = "${selectedCategory.icon} فيديوهات ${selectedCategory.title}",
                            subtitle = "تم العثور على ${categoryVideos.size} فيديو مطابق لهذا التصنيف",
                            videos = categoryVideos,
                            onVideoSelected = onVideoSelected
                        )
                    }
                }

                if (matchingFeeds.isNotEmpty()) {
                    items(matchingFeeds, key = { it.channel.channelId }) { feed ->
                        ChannelSection(
                            title = "📺 ${feed.channel.name}",
                            subtitle = "${feed.channel.category} • ${feed.videos.size} فيديو",
                            videos = feed.videos,
                            onVideoSelected = onVideoSelected
                        )
                    }
                }

                if (categoryVideos.isEmpty() && matchingFeeds.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${selectedCategory.icon} لا توجد فيديوهات في ${selectedCategory.title} حالياً",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يمكنك إضافة قنوات في هذا المجال أو تحديد تصنيف القناة في ملف channels.json",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isFocused -> Color.White
                    isSelected -> YoutubeRed
                    else -> YoutubeDarkGray
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.icon,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = category.title,
            color = when {
                isFocused -> Color.Black
                isSelected -> Color.White
                else -> TextPrimary
            },
            fontSize = 13.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ChannelSection(
    title: String,
    subtitle: String,
    videos: List<Video>,
    onVideoSelected: (Video) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp)
    ) {
        // عنوان القسم أو القناة
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // القائمة الأفقية لفيديوهات القسم
        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    onClick = onVideoSelected
                )
            }
        }
    }
}
