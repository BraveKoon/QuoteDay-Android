package com.quoteday.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
// 낮/밤 두 색을 받는 ColorProvider 는 androidx.glance.unit 이 아니라
// androidx.glance.appwidget.unit 에 있다. 이름이 같아서 잘못 가져오기 쉽다.
import androidx.glance.appwidget.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.quoteday.app.MainActivity
import com.quoteday.app.data.AppSettings
import com.quoteday.core.DeepLink
import com.quoteday.core.QuoteLibrary
import com.quoteday.core.QuoteService
import java.time.LocalDate

/**
 * 홈 화면 위젯 — 오늘의 명언.
 *
 * 앱 본체와 위젯은 **서로 다른 프로세스**에서 돈다. 그래서 위젯은 앱이 계산해
 * 둔 값을 건네받는 것이 아니라, 같은 규칙으로 **다시 계산**한다. 오늘의 명언이
 * 날짜에서 결정적으로 정해지는 것이 이 자리에서 값을 한다 — 어느 쪽이 먼저
 * 그리든 같은 문장이 나온다.
 */
class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = AppSettings(context)
        val library = QuoteLibrary.shared
        val presentation = QuoteService(library).presentationOfTheDay(
            date = LocalDate.now(),
            preferred = settings.preferredCategory,
        )

        provideContent {
            val open = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(DeepLink.Quote(presentation.quote.id).uri)
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(surface)
                    .padding(16.dp)
                    .clickable(actionStartActivity(open)),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "“${presentation.quote.text}”",
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    // 긴 명언은 잘린다. 위젯은 읽히는 곳이지 다 담는 곳이 아니다.
                    maxLines = 5,
                )
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "— ${presentation.author.displayName}",
                    style = TextStyle(color = textSecondary, fontSize = 12.sp),
                )
            }
        }
    }

    private companion object {
        // 앱 본체의 ClayTheme 와 같은 값이다. Compose 쪽 토큰을 그대로 쓸 수 없어서
        // (위젯은 별도 프로세스에서 다른 런타임으로 그린다) 여기에 한 번 더 적는다.
        val surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1C1F2C))
        val textPrimary = ColorProvider(day = Color(0xFF2E3350), night = Color(0xFFF1F3FF))
        val textSecondary = ColorProvider(day = Color(0xFF6E7595), night = Color(0xFFB3B9D6))
    }
}

/** 시스템이 위젯을 만들고 갱신할 때 부르는 자리. */
class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}
