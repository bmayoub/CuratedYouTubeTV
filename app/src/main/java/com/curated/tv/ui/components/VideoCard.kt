package com.curated.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.curated.tv.model.Video
import com.curated.tv.ui.theme.CardBackground
import com.curated.tv.ui.theme.FocusBorderColor
import com.curated.tv.ui.theme.TextPrimary
import com.curated.tv.ui.theme.TextSecondary
import com.curated.tv.ui.theme.YoutubeRed

@Composable
fun VideoCard(
    video: Video,
    onClick: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "card_scale")

    Column(
        modifier = modifier
            .width(260.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) CardBackground else Color.Transparent)
            .border(
                BorderStroke(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) FocusBorderColor else Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick(video) }
            .focusable()
            .padding(bottom = 8.dp)
    ) {
        // صورة غلاف الفيديو (Thumbnail)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // تفاصيل الفيديو
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = video.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = video.channelName,
                    color = if (isFocused) YoutubeRed else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = video.publishedDate,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

