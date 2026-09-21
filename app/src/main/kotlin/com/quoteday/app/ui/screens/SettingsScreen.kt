package com.quoteday.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.data.AppSettings
import com.quoteday.app.ui.ClayCard
import com.quoteday.app.ui.ClayChip
import com.quoteday.app.ui.ClayScreenHeader
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.TAB_BAR_INSET
import com.quoteday.core.AppCategory

/** 설정. */
@Composable
fun SettingsScreen(app: QuoteDayApplication) {
    val colors = ClayTheme.colors
    val settings = app.settings

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = ClaySpacing.m, end = ClaySpacing.m, top = ClaySpacing.m, bottom = TAB_BAR_INSET,
        ),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
    ) {
        item { ClayScreenHeader(title = "설정") }

        item {
            SettingsSection(
                title = "오늘의 명언",
                detail = "고른 카테고리에서 매일 한 편을 뽑습니다. 고르지 않으면 전체에서 뽑습니다.",
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    ClayChip(
                        text = "전체",
                        tint = if (settings.preferredCategory == null) colors.accent else colors.surfaceRaised,
                        textColor = if (settings.preferredCategory == null) {
                            colors.textOnAccent
                        } else {
                            colors.textSecondary
                        },
                        onClick = { settings.preferredCategory = null },
                    )
                    for (category in AppCategory.selectableForQuotes) {
                        val isOn = settings.preferredCategory == category
                        ClayChip(
                            text = category.displayName,
                            tint = if (isOn) colors.tint(category) else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnTint else colors.textSecondary,
                            onClick = { settings.preferredCategory = if (isOn) null else category },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = "화면", detail = "기기 설정을 따르거나, 한쪽으로 고정합니다.") {
                Row(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                    for (appearance in AppSettings.Appearance.entries) {
                        val isOn = settings.appearance == appearance
                        ClayChip(
                            text = appearance.title,
                            tint = if (isOn) colors.accent else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
                            onClick = { settings.appearance = appearance },
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(
                title = "하트",
                detail = "지금은 하트가 이 기기 안에만 쌓입니다. 다른 사람이 누른 수는 아직 보이지 않습니다.",
            ) {}
        }

        item {
            SettingsSection(title = "명언", detail = null) {
                Text(
                    "명언 ${app.library.count}편 · 인물 ${app.library.authors.size}명 · 배경 ${app.library.behindStories.size}편",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    "출처를 확인하지 못한 문장은 상세 화면에 그렇게 적어 둡니다. " +
                        "인물을 묻는 챌린지 문제로는 내지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        item {
            SettingsSection(title = "정보", detail = null) {
                Text(
                    "오늘의 명언 · 버전 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    detail: String?,
    content: @Composable () -> Unit,
) {
    val colors = ClayTheme.colors
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(ClaySpacing.m),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            content()
        }
    }
}
