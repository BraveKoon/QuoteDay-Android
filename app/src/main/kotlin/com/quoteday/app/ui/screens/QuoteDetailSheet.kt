package com.quoteday.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.ClayCard
import com.quoteday.app.ui.ClayChip
import com.quoteday.app.ui.ClayRadius
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.HeartButton
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * 명언 한 편의 상세.
 *
 * 출처가 확인된 명언에는 배경 이야기가 붙는다. 없는 명언에 억지로 채우지 않는다 —
 * 지어낸 배경은 앱이 조용히 틀린 역사를 가르치는 가장 흔한 경로다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailSheet(
    app: QuoteDayApplication,
    slug: String,
    onDismiss: () -> Unit,
) {
    val colors = ClayTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presentation = remember(slug) { app.library.quoteBySlug(slug)?.let { app.library.presentation(it) } }
    val story = remember(slug) { app.library.behindStory(slug) }

    if (presentation == null) {
        onDismiss()
        return
    }

    val quote = presentation.quote
    val author = presentation.author

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ClaySpacing.m)
                .padding(bottom = ClaySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.m),
        ) {
            ClayCard(cornerRadius = ClayRadius.hero) {
                Column(
                    Modifier.fillMaxWidth().padding(ClaySpacing.l),
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
                        HeartButton(
                            count = app.hearts.count(slug),
                            isOn = app.hearts.isHearted(slug),
                            onClick = { app.hearts.toggle(slug) },
                        )
                    }
                    Text(
                        "“${quote.text}”",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                    quote.originalText?.let { original ->
                        Text(
                            original,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            ClayCard {
                Column(
                    Modifier.fillMaxWidth().padding(ClaySpacing.m),
                    verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    Text(
                        author.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        listOfNotNull(author.nationality, author.occupation, author.lifespan)
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    Text(
                        author.biography,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = ClaySpacing.xs),
                    )
                    for (achievement in author.achievements) {
                        Text(
                            "· $achievement",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            if (story != null) {
                ClayCard(background = colors.surfaceRaised) {
                    Column(
                        Modifier.fillMaxWidth().padding(ClaySpacing.m),
                        verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                    ) {
                        Text(
                            "이 말이 나온 자리",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            story.occasion,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary,
                        )
                        Text(
                            story.context,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        story.takeaway?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                        Text(
                            "출처: ${story.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else if (app.library.isDisputed(slug)) {
                // 널리 인용되지만 1차 출처를 찾지 못한 문장이다. 숨기지 않고 말해 준다.
                ClayCard(background = colors.surfaceRaised) {
                    Text(
                        "널리 인용되지만 이 인물이 실제로 말했다는 기록은 확인되지 않았습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth().padding(ClaySpacing.m),
                    )
                }
            }

            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, presentation.shareText)
                    }
                    context.startActivity(Intent.createChooser(send, "명언 공유"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ClayRadius.control),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                ),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text("공유하기", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
