package com.curated.tv.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.curated.tv.ui.theme.YoutubeBlack

class PlayerActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var videoId: String = ""

    companion object {
        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: ""
        if (videoId.isEmpty()) {
            finish()
            return
        }

        // إخفاء أشرطة النظام بالكامل للتلفاز
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        webView = WebView(this).apply {
            setBackgroundColor(0xFF0F0F0F.toInt())
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 TV Safari/538.1"
            }
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
        }

        setContentView(webView)
        loadYouTubePlayer(videoId)
    }

    private fun loadYouTubePlayer(id: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background-color: #0F0F0F; overflow: hidden; }
                    html, body { width: 100%; height: 100%; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$id',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 1,
                                'rel': 0,
                                'fs': 0,
                                'modestbranding': 1,
                                'playsinline': 1,
                                'iv_load_policy': 3
                            },
                            events: {
                                'onReady': onPlayerReady
                            }
                        });
                    }
                    function onPlayerReady(event) {
                        event.target.playVideo();
                    }
                    function togglePlay() {
                        if (player.getPlayerState() == 1) {
                            player.pauseVideo();
                        } else {
                            player.playVideo();
                        }
                    }
                    function seekRelative(seconds) {
                        var curTime = player.getCurrentTime();
                        player.seekTo(curTime + seconds, true);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
    }

    /**
     * التحكم بريموت التلفاز (D-Pad Controller)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // زر الاختيار / التشغيل والإيقاف (OK / D-Pad Center)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                webView.evaluateJavascript("togglePlay();", null)
                true
            }
            // سهم يمين: تقديم 10 ثوانٍ
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                webView.evaluateJavascript("seekRelative(10);", null)
                true
            }
            // سهم يسار: تأخير 10 ثوانٍ
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                webView.evaluateJavascript("seekRelative(-10);", null)
                true
            }
            // زر الرجوع في الريموت
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}

