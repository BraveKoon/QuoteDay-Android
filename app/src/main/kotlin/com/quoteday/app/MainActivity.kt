package com.quoteday.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quoteday.app.data.AppSettings
import com.quoteday.app.ui.QuoteDayTheme
import com.quoteday.app.ui.RootScreen

class MainActivity : ComponentActivity() {

    /** 위젯이나 알림이 열어 준 명언. 없으면 null. */
    private var pendingQuoteId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingQuoteId = quoteId(intent)

        val app = application as QuoteDayApplication

        setContent {
            val darkTheme = when (app.settings.appearance) {
                AppSettings.Appearance.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                AppSettings.Appearance.LIGHT -> false
                AppSettings.Appearance.DARK -> true
            }
            QuoteDayTheme(darkTheme = darkTheme) {
                RootScreen(
                    app = app,
                    openQuoteId = pendingQuoteId,
                    onOpenedQuote = { pendingQuoteId = null },
                )
            }
        }
    }

    /**
     * 앱이 이미 떠 있는 채로 딥링크가 들어오면 `onCreate` 가 아니라 여기로 온다.
     * 이 경로를 빠뜨리면 위젯을 눌러도 화면이 그대로인 버그가 된다.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quoteId(intent)?.let { pendingQuoteId = it }
    }

    /** `quoteday://quote/<id>` 에서 명언 식별자를 꺼낸다. */
    private fun quoteId(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_QUOTE_HOST) return null
        return uri.pathSegments.firstOrNull()
    }

    companion object {
        const val DEEP_LINK_SCHEME = "quoteday"
        const val DEEP_LINK_QUOTE_HOST = "quote"
    }
}
