package com.quoteday.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.screens.ChallengeScreen
import com.quoteday.app.ui.screens.HomeScreen
import com.quoteday.app.ui.screens.QuoteBrowserScreen
import com.quoteday.app.ui.screens.QuoteDetailSheet
import com.quoteday.app.ui.screens.ScheduleScreen
import com.quoteday.app.ui.screens.SettingsScreen

/** 아래 탭. */
enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("오늘", Icons.Filled.WbSunny),
    SCHEDULE("일정", Icons.Filled.CalendarMonth),
    QUOTES("명언", Icons.Filled.FormatQuote),
    CHALLENGE("챌린지", Icons.Filled.EmojiEvents),
    SETTINGS("설정", Icons.Filled.Settings),
}

/**
 * 앱의 최상위 화면.
 *
 * 화면 전환에 Navigation 라이브러리를 쓰지 않는다. 탭 넷과 시트 하나가 전부라,
 * 백스택을 관리해 줄 것이 없다. iOS 쪽도 같은 이유로 `RootTabView` 하나다.
 */
@Composable
fun RootScreen(
    app: QuoteDayApplication,
    openQuoteId: String? = null,
    onOpenedQuote: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var presentedQuoteSlug by remember { mutableStateOf<String?>(null) }

    // 위젯·알림이 열어 준 명언을 시트로 띄운다.
    LaunchedEffect(openQuoteId) {
        val id = openQuoteId ?: return@LaunchedEffect
        app.library.quoteById(id)?.let { presentedQuoteSlug = it.slug }
        onOpenedQuote()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ClayTheme.colors.background)
    ) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
        ) { current ->
            when (current) {
                AppTab.HOME -> HomeScreen(
                    app = app,
                    onOpenQuote = { presentedQuoteSlug = it },
                    onOpenQuotes = { tab = AppTab.QUOTES },
                    onOpenSchedules = { tab = AppTab.SCHEDULE },
                )
                AppTab.SCHEDULE -> ScheduleScreen(
                    app = app,
                    onOpenQuote = { presentedQuoteSlug = it },
                )
                AppTab.QUOTES -> QuoteBrowserScreen(app = app, onOpenQuote = { presentedQuoteSlug = it })
                AppTab.CHALLENGE -> ChallengeScreen(app = app)
                AppTab.SETTINGS -> SettingsScreen(app = app)
            }
        }

        ClayTabBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = ClaySpacing.m, vertical = ClaySpacing.xs),
        )
    }

    val slug = presentedQuoteSlug
    if (slug != null) {
        QuoteDetailSheet(app = app, slug = slug, onDismiss = { presentedQuoteSlug = null })
    }
}

@Composable
private fun ClayTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClayCard(modifier = modifier.fillMaxWidth(), cornerRadius = ClayRadius.card) {
        Row(
            Modifier.fillMaxWidth().padding(ClaySpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
        ) {
            for (item in AppTab.entries) {
                TabButton(
                    tab = item,
                    isSelected = item == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { if (item != selected) onSelect(item) },
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: AppTab,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = ClayTheme.colors
    val shape = RoundedCornerShape(ClayRadius.control)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = ClaySpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.title,
            tint = if (isSelected) colors.textOnAccent else colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.textOnAccent else colors.textSecondary,
        )
    }
}
