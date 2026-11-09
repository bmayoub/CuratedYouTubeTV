package com.curated.tv.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class PlayerActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var videoId: String = ""

    companion object {
        const val EXTRA_VIDEO_ID = "extra_video_id"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }

    class WebAppInterface(private val activity: ComponentActivity, private val videoId: String) {
        @JavascriptInterface
        fun onPlayerError(errorCode: Int) {
            // خطأ 150 / 152 / 101 يعني أن صاحب الفيديو حظر تشغيله خارج منصة يوتيوب
            if (errorCode == 150 || errorCode == 152 || errorCode == 101) {
                activity.runOnUiThread {
                    openInYouTubeApp()
                }
            }
        }

        @JavascriptInterface
        fun openInYouTubeApp() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).apply {
                    setPackage("com.google.android.youtube.tv")
                }
                activity.startActivity(intent)
                activity.finish()
            } catch (e: Throwable) {
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                    activity.startActivity(fallback)
                    activity.finish()
                } catch (_: Throwable) {}
            }
        }
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

        // تفعيل ملفات تعريف الارتباط للطرف الثالث الضرورية لمشغل يوتيوب
        try {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
            }
        } catch (_: Throwable) {}

        webView = WebView(this).apply {
            setBackgroundColor(0xFF0F0F0F.toInt())
            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (_: Throwable) {}

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // استخدام User Agent حديث ومتوافق تماماً مع مشغل يوتيوب
                userAgentString = "Mozilla/5.0 (Linux; Android 12; Chromecast Build/STTE.231215.005) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            }
            addJavascriptInterface(WebAppInterface(this@PlayerActivity, videoId), "AndroidBridge")
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
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; background-color: #0F0F0F; overflow: hidden; }
                    html, body { width: 100%; height: 100%; background-color: #0F0F0F; }
                    #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
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
                            host: 'https://www.youtube-nocookie.com',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 1,
                                'rel': 0,
                                'fs': 0,
                                'modestbranding': 1,
                                'playsinline': 1,
                                'enablejsapi': 1,
                                'origin': 'https://www.youtube.com',
                                'widget_referrer': 'https://www.youtube.com',
                                'iv_load_policy': 3
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onError': onPlayerError
                            }
                        });
                    }
                    function onPlayerReady(event) {
                        event.target.playVideo();
                    }
                    function onPlayerError(event) {
                        console.log("Player error:", event.data);
                        if (window.AndroidBridge && (event.data === 150 || event.data === 152 || event.data === 101 || event.data === 2)) {
                            window.AndroidBridge.onPlayerError(event.data);
                        }
                    }
                    function togglePlay() {
                        if (!player || typeof player.getPlayerState !== 'function') return;
                        if (player.getPlayerState() == 1) {
                            player.pauseVideo();
                        } else {
                            player.playVideo();
                        }
                    }
                    function seekRelative(seconds) {
                        if (!player || typeof player.getCurrentTime !== 'function') return;
                        var curTime = player.getCurrentTime();
                        player.seekTo(curTime + seconds, true);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", "https://www.youtube.com")
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
            // زر القائمة / زر إضافي للفتح المباشر في يوتيوب الرسمي إن رغب المستخدم
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_INFO -> {
                WebAppInterface(this, videoId).openInYouTubeApp()
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
