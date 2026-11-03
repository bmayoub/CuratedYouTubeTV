package com.curated.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.curated.tv.ui.theme.TextPrimary
import com.curated.tv.ui.theme.TextSecondary
import com.curated.tv.ui.theme.YoutubeDarkGray
import com.curated.tv.ui.theme.YoutubeRed

enum class NavItem(val title: String, val iconText: String) {
    HOME("الرئيسية", "🏠"),
    CHANNELS("القنوات", "📺"),
    REFRESH("تحديث", "🔄"),
    SETTINGS("الإعدادات", "⚙️")
}

@Composable
fun Sidebar(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFocusInside by remember { mutableStateOf(false) }
    val width by animateDpAsState(targetValue = if (hasFocusInside) 180.dp else 70.dp, label = "sidebar_width")

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(YoutubeDarkGray)
            .padding(vertical = 24.dp, horizontal = 12.dp)
            .onFocusChanged { hasFocusInside = it.hasFocus },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // الشعار (YouTube style logo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(YoutubeRed),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "▶", color = Color.White, fontSize = 16.sp)
            }
            if (hasFocusInside) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Curated TV",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // عناصر القائمة
        NavItem.entries.forEach { item ->
            SidebarItem(
                item = item,
                isSelected = selectedItem == item,
                expanded = hasFocusInside,
                onClick = { onItemSelected(item) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SidebarItem(
    item: NavItem,
    isSelected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFocused -> Color.White
                    isSelected -> YoutubeRed.copy(alpha = 0.25f)
                    else -> Color.Transparent
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.iconText,
            fontSize = 20.sp
        )

        if (expanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.title,
                color = if (isFocused) Color.Black else if (isSelected) YoutubeRed else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

