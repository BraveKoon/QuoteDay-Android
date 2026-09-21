package com.quoteday.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 클레이 카드.
 *
 * 그림자 대신 **배경과의 밝기 차이 + 얇은 테두리**로 층을 나눈다. 안드로이드의
 * `Card` 를 쓰지 않는 이유가 여기 있다 — 기본 elevation 그림자가 이 규칙을 깬다.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = ClayRadius.card,
    background: Color = ClayTheme.colors.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val base = modifier
        .clip(shape)
        .background(background)
        .border(BorderStroke(1.dp, ClayTheme.colors.separator), shape)
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content,
    )
}

/** 카테고리·상태를 나타내는 작은 칩. */
@Composable
fun ClayChip(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = ClayTheme.colors.surfaceRaised,
    textColor: Color = ClayTheme.colors.textSecondary,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(ClayRadius.chip)
    val base = modifier.clip(shape).background(tint)
    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = ClaySpacing.s, vertical = ClaySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

/** 화면 맨 위의 제목 줄. */
@Composable
fun ClayScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = ClayTheme.colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClayTheme.colors.textSecondary,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** 목록이 비었을 때의 안내. */
@Composable
fun ClayEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    ClayCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(ClaySpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
        ) {
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = ClayTheme.colors.textPrimary,
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClayTheme.colors.textSecondary,
                )
            }
        }
    }
}

/**
 * 스크롤 내용이 탭 바 아래로 숨지 않도록 두는 아래 여백.
 *
 * 탭 바가 화면 위에 떠 있어서(`Box` 의 맨 아래) 목록이 그 밑으로 흘러간다.
 * 이 값을 빠뜨리면 마지막 카드가 영원히 가려진다.
 */
val TAB_BAR_INSET: Dp = 110.dp
