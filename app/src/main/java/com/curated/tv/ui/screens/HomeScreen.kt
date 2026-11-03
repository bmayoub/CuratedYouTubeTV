package com.curated.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.curated.tv.model.ChannelFeed
import com.curated.tv.model.Video
import com.curated.tv.ui.components.VideoCard
import com.curated.tv.ui.theme.TextPrimary
import com.curated.tv.ui.theme.TextSecondary
import com.curated.tv.ui.theme.YoutubeBlack
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

    // تجميع أحدث الفيديوهات من كل القنوات لصف البداية
    val allRecentVideos = remember(feeds) {
        feeds.flatMap { it.videos }
            .sortedByDescending { it.publishedDate }
            .take(20)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(YoutubeBlack),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
    ) {
        // صف أحدث الفيديوهات المجمعة
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

        // صف مستقل لكل قناة مخصصة
        items(feeds) { feed ->
            ChannelSection(
                title = "📺 ${feed.channel.name}",
                subtitle = "${feed.channel.category} • ${feed.videos.size} فيديو متوفر",
                videos = feed.videos,
                onVideoSelected = onVideoSelected
            )
        }
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
        // عنوان القناة
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

        // القائمة الأفقية لفيديوهات القناة
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
