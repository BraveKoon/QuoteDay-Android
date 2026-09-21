package com.quoteday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.quoteday.core.QuotePresentation

/** 명언 카드의 크기. */
enum class QuoteCardStyle { HERO, COMPACT }

/**
 * 명언 한 편을 담는 카드.
 *
 * 따옴표를 문장에 직접 넣지 않고 카드가 그린다. 데이터에 따옴표를 넣으면 공유
 * 텍스트나 검색에까지 따라다닌다.
 */
@Composable
fun QuoteCard(
    presentation: QuotePresentation,
    modifier: Modifier = Modifier,
    style: QuoteCardStyle = QuoteCardStyle.COMPACT,
    heartCount: Int? = null,
    isHearted: Boolean = false,
    onHeart: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = ClayTheme.colors
    val quote = presentation.quote

    ClayCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = if (style == QuoteCardStyle.HERO) ClayRadius.hero else ClayRadius.card,
        onClick = onClick,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(if (style == QuoteCardStyle.HERO) ClaySpacing.l else ClaySpacing.m),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClayChip(
                    text = quote.category.displayName,
                    tint = colors.tint(quote.category),
                    textColor = colors.textOnTint,
                )
                Spacer(Modifier.weight(1f))
                if (onHeart != null) {
                    HeartButton(count = heartCount ?: 0, isOn = isHearted, onClick = onHeart)
                }
            }

            Text(
                text = "“${quote.text}”",
                style = if (style == QuoteCardStyle.HERO) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = colors.textPrimary,
                maxLines = if (style == QuoteCardStyle.HERO) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "— ${presentation.author.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun HeartButton(
    count: Int,
    isOn: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = ClayTheme.colors
    ClayChip(
        text = if (count > 0) count.toString() else "하트",
        modifier = modifier,
        tint = if (isOn) colors.danger else colors.surfaceRaised,
        textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
        onClick = onClick,
        icon = if (isOn) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
    )
}
