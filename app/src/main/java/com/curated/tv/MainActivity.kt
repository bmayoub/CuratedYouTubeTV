package com.curated.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.curated.tv.data.ChannelRepository
import com.curated.tv.model.ChannelFeed
import com.curated.tv.model.Video
import com.curated.tv.ui.components.NavItem
import com.curated.tv.ui.components.Sidebar
import com.curated.tv.ui.screens.HomeScreen
import com.curated.tv.ui.screens.PlayerActivity
import com.curated.tv.ui.theme.CuratedTVTheme
import com.curated.tv.ui.theme.YoutubeBlack
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: ChannelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ChannelRepository(this)

        setContent {
            CuratedTVTheme {
                val coroutineScope = rememberCoroutineScope()
                var feeds by remember { mutableStateOf<List<ChannelFeed>>(emptyList()) }
                var isLoading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf("") }
                var selectedNav by remember { mutableStateOf(NavItem.HOME) }

                fun refreshFeeds() {
                    isLoading = true
                    errorMessage = ""
                    coroutineScope.launch {
                        feeds = repository.fetchAllFeeds()
                        errorMessage = repository.lastErrorMessage
                        isLoading = false
                    }
                }

                LaunchedEffect(Unit) {
                    refreshFeeds()
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(YoutubeBlack)
                ) {
                    // القائمة الجانبية (Sidebar)
                    Sidebar(
                        selectedItem = selectedNav,
                        onItemSelected = { item ->
                            selectedNav = item
                            if (item == NavItem.REFRESH) {
                                refreshFeeds()
                                selectedNav = NavItem.HOME
                            }
                        }
                    )

                    // المحتوى الرئيسي (Home Rows)
                    HomeScreen(
                        feeds = feeds,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onRetry = { refreshFeeds() },
                        onVideoSelected = { video: Video ->
                            val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_VIDEO_ID, video.id)
                                putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, video.title)
                            }
                            startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
